package com.grafie.botjava.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 群指令调用记录的保留与统计配置。
 */
@Component
@ConfigurationProperties(prefix = "bot.command.audit")
public class CommandAuditProperties {

    private int retentionDays = 30;
    private int statisticsDefaultDays = 7;
    private int statisticsMaxDays = 90;

    public int getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(int retentionDays) {
        this.retentionDays = requirePositive(retentionDays, "retention-days");
    }

    public int getStatisticsDefaultDays() {
        return statisticsDefaultDays;
    }

    public void setStatisticsDefaultDays(int statisticsDefaultDays) {
        this.statisticsDefaultDays = requirePositive(statisticsDefaultDays, "statistics-default-days");
    }

    public int getStatisticsMaxDays() {
        return statisticsMaxDays;
    }

    public void setStatisticsMaxDays(int statisticsMaxDays) {
        this.statisticsMaxDays = requirePositive(statisticsMaxDays, "statistics-max-days");
    }

    private int requirePositive(int value, String property) {
        if (value < 1) {
            throw new IllegalArgumentException("bot.command.audit." + property + " 必须大于 0");
        }
        return value;
    }
}
