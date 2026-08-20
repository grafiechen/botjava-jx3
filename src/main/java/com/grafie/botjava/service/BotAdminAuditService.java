package com.grafie.botjava.service;

import com.grafie.botjava.entity.BotAdminAuditLog;
import com.grafie.botjava.mapper.BotAdminAuditLogMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BotAdminAuditService {

    private final BotAdminAuditLogMapper auditLogMapper;

    public BotAdminAuditService(BotAdminAuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    public void record(String category, String action, String targetKey, String actorOpenid,
                       String actorSource, boolean success, String failureSummary) {
        BotAdminAuditLog log = new BotAdminAuditLog();
        log.setCategory(limit(category, 32));
        log.setAction(limit(action, 64));
        log.setTargetKey(limit(targetKey, 128));
        log.setActorOpenid(limit(actorOpenid, 128));
        log.setActorSource(limit(actorSource, 32));
        log.setSuccess(success);
        log.setFailureSummary(limit(failureSummary, 256));
        auditLogMapper.save(log);
    }

    public List<BotAdminAuditLog> recent() {
        return auditLogMapper.findTop10ByOrderByCreateTimeDesc();
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}