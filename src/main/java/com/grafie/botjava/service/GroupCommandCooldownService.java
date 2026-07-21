package com.grafie.botjava.service;

import com.grafie.botjava.config.CommandCooldownProperties;
import com.grafie.botjava.entity.GroupCommandCooldown;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.mapper.GroupCommandCooldownMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * 通过数据库原子占位实现按群和指令隔离的跨实例冷却。
 */
@Service
public class GroupCommandCooldownService {

    private final CommandCooldownProperties properties;
    private final GroupCommandCooldownMapper cooldownMapper;
    private final Clock clock;

    @Autowired
    public GroupCommandCooldownService(CommandCooldownProperties properties,
                                       GroupCommandCooldownMapper cooldownMapper) {
        this(properties, cooldownMapper, Clock.systemUTC());
    }

    public GroupCommandCooldownService(CommandCooldownProperties properties,
                                       GroupCommandCooldownMapper cooldownMapper,
                                       Clock clock) {
        this.properties = properties;
        this.cooldownMapper = cooldownMapper;
        this.clock = clock;
    }

    public Decision tryAcquire(String groupOpenId, REGEX definition) {
        requireKey(groupOpenId, definition);
        Duration cooldown = properties.getCooldown(definition);
        if (cooldown.isZero()) {
            return Decision.allowWithoutReservation();
        }

        Instant now = clock.instant();
        String reservationId = UUID.randomUUID().toString();
        Instant leaseExpiresAt = now.plus(properties.getInFlightTimeout());
        if (cooldownMapper.reserveExisting(
                groupOpenId, definition.name(), reservationId, now, leaseExpiresAt) == 1) {
            return Decision.allow(groupOpenId, definition, reservationId, cooldown);
        }

        GroupCommandCooldown existing = cooldownMapper.findByGroupOpenIdAndCommandName(
                groupOpenId, definition.name());
        if (existing != null) {
            return Decision.deny(retryAfterSeconds(existing, now, cooldown));
        }

        if (insertReservation(groupOpenId, definition, reservationId, now, leaseExpiresAt)) {
            return Decision.allow(groupOpenId, definition, reservationId, cooldown);
        }

        // 插入竞争失败后再尝试一次条件更新，可接管刚好过期的旧记录。
        if (cooldownMapper.reserveExisting(
                groupOpenId, definition.name(), reservationId, now, leaseExpiresAt) == 1) {
            return Decision.allow(groupOpenId, definition, reservationId, cooldown);
        }
        return Decision.deny(retryAfterSeconds(groupOpenId, definition, now, cooldown));
    }

    /**
     * Action 和 QQ 发送结束后，仅由持有相同 UUID 的调用开始冷却计时。
     */
    public void complete(Decision decision) {
        if (decision == null || !decision.allowed() || decision.reservationId() == null) {
            return;
        }
        Instant completedAt = clock.instant();
        cooldownMapper.completeReservation(
                decision.groupOpenId(), decision.definition().name(), decision.reservationId(),
                completedAt, completedAt.plus(decision.cooldown()));
    }

    private boolean insertReservation(String groupOpenId, REGEX definition, String reservationId,
                                      Instant now, Instant leaseExpiresAt) {
        GroupCommandCooldown state = new GroupCommandCooldown();
        state.setGroupOpenId(groupOpenId);
        state.setCommandName(definition.name());
        state.setReservationId(reservationId);
        state.setReservedAt(now);
        state.setLeaseExpiresAt(leaseExpiresAt);
        try {
            cooldownMapper.saveAndFlush(state);
            return true;
        } catch (DataIntegrityViolationException ignored) {
            return false;
        }
    }

    private long retryAfterSeconds(String groupOpenId, REGEX definition, Instant now, Duration cooldown) {
        GroupCommandCooldown state = cooldownMapper.findByGroupOpenIdAndCommandName(
                groupOpenId, definition.name());
        return retryAfterSeconds(state, now, cooldown);
    }

    private long retryAfterSeconds(GroupCommandCooldown state, Instant now, Duration cooldown) {
        if (state == null) {
            return Math.max(1, cooldown.toSeconds());
        }
        Instant blockedUntil = state.getReservationId() == null
                ? state.getAvailableAt() : state.getLeaseExpiresAt();
        if (blockedUntil == null) {
            return Math.max(1, cooldown.toSeconds());
        }
        long millis = Duration.between(now, blockedUntil).toMillis();
        return Math.max(1, (millis + 999) / 1_000);
    }

    private void requireKey(String groupOpenId, REGEX definition) {
        if (groupOpenId == null || groupOpenId.isBlank()) {
            throw new IllegalArgumentException("群指令冷却需要 groupOpenId");
        }
        if (definition == null) {
            throw new IllegalArgumentException("群指令冷却需要指令定义");
        }
    }

    public record Decision(boolean allowed, long retryAfterSeconds, String groupOpenId,
                           REGEX definition, String reservationId, Duration cooldown) {

        public static Decision allow(String groupOpenId, REGEX definition,
                                     String reservationId, Duration cooldown) {
            return new Decision(true, 0, groupOpenId, definition, reservationId, cooldown);
        }

        public static Decision allowWithoutReservation() {
            return new Decision(true, 0, null, null, null, Duration.ZERO);
        }

        public static Decision deny(long retryAfterSeconds) {
            return new Decision(false, retryAfterSeconds, null, null, null, Duration.ZERO);
        }
    }
}
