package com.grafie.botjava.service;

import com.grafie.botjava.config.CommandAuditProperties;
import com.grafie.botjava.entity.CommandInvocationStatus;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.mapper.CommandInvocationMapper;
import com.grafie.botjava.mapper.CommandInvocationSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CommandInvocationQueryService {

    private static final int MAX_DISPLAY_ROWS = 10;

    private final CommandInvocationMapper mapper;
    private final CommandAuditProperties properties;
    private final Clock clock;

    @Autowired
    public CommandInvocationQueryService(CommandInvocationMapper mapper, CommandAuditProperties properties) {
        this(mapper, properties, Clock.systemDefaultZone());
    }

    CommandInvocationQueryService(CommandInvocationMapper mapper, CommandAuditProperties properties, Clock clock) {
        this.mapper = mapper;
        this.properties = properties;
        this.clock = clock;
    }

    public Statistics statistics(String groupOpenId, Integer requestedDays) {
        if (groupOpenId == null || groupOpenId.isBlank()) {
            throw new IllegalArgumentException("当前消息缺少群标识，无法查询调用统计。");
        }
        int days = requestedDays == null ? properties.getStatisticsDefaultDays() : requestedDays;
        if (days < 1 || days > properties.getStatisticsMaxDays()) {
            throw new IllegalArgumentException("统计天数必须在 1 至 " + properties.getStatisticsMaxDays() + " 之间。");
        }
        LocalDateTime since = LocalDateTime.now(clock).minusDays(days);
        List<CommandInvocationSummary> summaries = mapper.summarizeByGroup(
                groupOpenId, since, CommandInvocationStatus.SUCCESS);
        List<CommandStat> rows = summaries.stream()
                .limit(MAX_DISPLAY_ROWS)
                .map(this::toStat)
                .toList();
        return new Statistics(days, rows);
    }

    private CommandStat toStat(CommandInvocationSummary summary) {
        REGEX definition = REGEX.findByName(summary.getCommandName());
        String displayName = definition == null ? summary.getCommandName() : definition.getDisplayName();
        return new CommandStat(displayName, summary.getInvocationCount(), summary.getSuccessCount(),
                Math.round(summary.getAverageElapsedMillis()));
    }

    public record Statistics(int days, List<CommandStat> rows) {
    }

    public record CommandStat(String displayName, long invocationCount, long successCount,
                              long averageElapsedMillis) {
        public long successRate() {
            return invocationCount == 0 ? 0 : Math.round(successCount * 100.0 / invocationCount);
        }
    }
}
