package com.grafie.botjava.service;

import com.grafie.botjava.entity.PushEventReceipt;
import com.grafie.botjava.mapper.PushEventReceiptMapper;
import com.grafie.botjava.service.push.PushTaskDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 为 WS 事件提供跨线程、跨进程的持久化去重。
 */
@Slf4j
@Service
public class PushEventDeduplicationService {

    private final PushEventReceiptMapper mapper;
    private final int retentionDays;

    public PushEventDeduplicationService(PushEventReceiptMapper mapper,
                                         @Value("${bot.push.ws-dedup-retention-days:7}") int retentionDays) {
        if (retentionDays < 1 || retentionDays > 365) {
            throw new IllegalStateException("bot.push.ws-dedup-retention-days 必须在 1 到 365 之间");
        }
        this.mapper = mapper;
        this.retentionDays = retentionDays;
    }

    /**
     * 成功插入唯一占位才允许推送；重复键代表该事件已处理。
     */
    public boolean tryClaim(PushTaskDefinition task, String eventFingerprint) {
        if (task == null || eventFingerprint == null || !eventFingerprint.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("WS 推送任务或事件指纹不合法");
        }
        PushEventReceipt receipt = new PushEventReceipt();
        receipt.setTaskCode(task.code());
        receipt.setEventFingerprint(eventFingerprint);
        try {
            mapper.saveAndFlush(receipt);
            return true;
        } catch (DataIntegrityViolationException duplicate) {
            log.info("跳过已处理的 WS 推送事件，taskCode=>{}，eventFingerprint=>{}",
                    task.code(), eventFingerprint);
            return false;
        }
    }

    @Scheduled(cron = "${bot.push.ws-dedup-cleanup-cron:0 30 3 * * *}")
    public void cleanup() {
        int deleted = mapper.deleteCreatedBefore(LocalDateTime.now().minusDays(retentionDays));
        if (deleted > 0) {
            log.info("已清理过期 WS 推送去重记录，deleted=>{}，retentionDays=>{}",
                    deleted, retentionDays);
        }
    }
}