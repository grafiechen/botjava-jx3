package com.grafie.botjava.service;

import com.grafie.botjava.config.ActiveMessageProperties;
import com.grafie.botjava.entity.GroupInfo;
import com.grafie.botjava.mapper.GroupInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * 群主动消息授权与独立频控。
 */
@Service
public class GroupActiveMessagePolicy {

    private final GroupInfoMapper groupInfoMapper;
    private final ActiveMessageProperties properties;
    private final Clock clock;

    @Autowired
    public GroupActiveMessagePolicy(GroupInfoMapper groupInfoMapper, ActiveMessageProperties properties) {
        this(groupInfoMapper, properties, Clock.systemDefaultZone());
    }

    GroupActiveMessagePolicy(GroupInfoMapper groupInfoMapper, ActiveMessageProperties properties, Clock clock) {
        this.groupInfoMapper = groupInfoMapper;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public Permit acquire(String groupOpenId) {
        if (groupOpenId == null || groupOpenId.isBlank()) {
            return Permit.denied("群主动消息缺少 groupOpenId。", 0);
        }
        Instant now = clock.instant().truncatedTo(ChronoUnit.MILLIS);
        int interval = properties.getActiveMessageMinIntervalSeconds();
        String reservationId = UUID.randomUUID().toString();
        int reserved = groupInfoMapper.reserveActiveMessage(
                groupOpenId, now, now.minusSeconds(interval), reservationId);
        if (reserved == 1) {
            return Permit.allowed(groupOpenId, now, reservationId);
        }

        GroupInfo groupInfo = groupInfoMapper.findByOpenGroupId(groupOpenId);
        if (groupInfo == null || !Boolean.TRUE.equals(groupInfo.getActiveMessagesEnabled())) {
            return Permit.denied("本群未开启主动消息。", 0);
        }
        if (!Boolean.TRUE.equals(groupInfo.getActiveMessagesPlatformAllowed())) {
            return Permit.denied("QQ 平台尚未允许向本群发送主动消息。", 0);
        }
        Instant previous = groupInfo.getActiveMessageReservedAt();
        if (previous != null) {
            Instant availableAt = previous.plusSeconds(interval);
            if (availableAt.isAfter(now)) {
                return Permit.denied("本群主动消息处于频控期。", remainingSeconds(now, availableAt));
            }
        }
        return Permit.denied("本群主动消息发送窗口暂时不可用，请稍后重试。", 1);
    }

    @Transactional
    public void rollback(Permit permit) {
        if (permit != null && permit.allowed() && permit.groupOpenId() != null
                && permit.reservationId() != null) {
            groupInfoMapper.rollbackActiveMessage(permit.groupOpenId(), permit.reservationId());
        }
    }

    private long remainingSeconds(Instant now, Instant availableAt) {
        long millis = Duration.between(now, availableAt).toMillis();
        return Math.max(1, (millis + 999) / 1000);
    }

    public record Permit(boolean allowed, String message, long retryAfterSeconds,
                         String groupOpenId, Instant reservedAt, String reservationId) {
        public static Permit allowed(String groupOpenId, Instant reservedAt, String reservationId) {
            return new Permit(true, null, 0, groupOpenId, reservedAt, reservationId);
        }

        public static Permit denied(String message, long retryAfterSeconds) {
            return new Permit(false, message, Math.max(0, retryAfterSeconds), null, null, null);
        }
    }
}
