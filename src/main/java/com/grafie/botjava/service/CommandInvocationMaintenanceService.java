package com.grafie.botjava.service;

import com.grafie.botjava.config.CommandAuditProperties;
import com.grafie.botjava.mapper.CommandInvocationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Service
public class CommandInvocationMaintenanceService {

    private final CommandInvocationMapper mapper;
    private final CommandAuditProperties properties;
    private final Clock clock;

    @Autowired
    public CommandInvocationMaintenanceService(CommandInvocationMapper mapper, CommandAuditProperties properties) {
        this(mapper, properties, Clock.systemDefaultZone());
    }

    CommandInvocationMaintenanceService(CommandInvocationMapper mapper, CommandAuditProperties properties,
                                        Clock clock) {
        this.mapper = mapper;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(cron = "${bot.command.audit.cleanup-cron:0 15 3 * * *}")
    @Transactional
    public long cleanupExpired() {
        LocalDateTime threshold = LocalDateTime.now(clock).minusDays(properties.getRetentionDays());
        long deleted = mapper.deleteByCreateTimeBefore(threshold);
        if (deleted > 0) {
            log.info("清理过期群指令调用记录，retentionDays=>{}，deleted=>{}",
                    properties.getRetentionDays(), deleted);
        }
        return deleted;
    }
}
