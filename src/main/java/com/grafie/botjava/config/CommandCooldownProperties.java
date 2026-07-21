package com.grafie.botjava.config;

import com.grafie.botjava.jx3.http.util.REGEX;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 群指令冷却配置。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "bot.command")
public class CommandCooldownProperties {

    private static final int MAX_COOLDOWN_SECONDS = 86_400;
    private static final int MIN_IN_FLIGHT_TIMEOUT_SECONDS = 30;
    private static final int MAX_IN_FLIGHT_TIMEOUT_SECONDS = 3_600;

    /**
     * 按 REGEX 枚举名覆盖冷却秒数，0 表示关闭该指令冷却。
     */
    private Map<String, Integer> cooldownSeconds = new LinkedHashMap<>();

    /**
     * 执行实例异常退出后，数据库占位自动失效的最长时间。
     */
    private int cooldownInFlightTimeoutSeconds = 300;

    @PostConstruct
    public void validate() {
        if (cooldownInFlightTimeoutSeconds < MIN_IN_FLIGHT_TIMEOUT_SECONDS
                || cooldownInFlightTimeoutSeconds > MAX_IN_FLIGHT_TIMEOUT_SECONDS) {
            throw new IllegalArgumentException(
                    "bot.command.cooldown-in-flight-timeout-seconds 必须在 30 到 3600 之间");
        }
        for (Map.Entry<String, Integer> entry : cooldownSeconds.entrySet()) {
            findDefinition(entry.getKey());
            Integer seconds = entry.getValue();
            if (seconds == null || seconds < 0 || seconds > MAX_COOLDOWN_SECONDS) {
                throw new IllegalArgumentException(
                        "bot.command.cooldown-seconds." + entry.getKey() + " 必须在 0 到 86400 之间");
            }
        }
    }

    public Duration getCooldown(REGEX definition) {
        Integer override = cooldownSeconds.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(definition.name()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
        return Duration.ofSeconds(override == null ? definition.getDefaultCooldownSeconds() : override);
    }

    public Duration getInFlightTimeout() {
        return Duration.ofSeconds(cooldownInFlightTimeoutSeconds);
    }

    private REGEX findDefinition(String name) {
        for (REGEX definition : REGEX.values()) {
            if (definition.name().equalsIgnoreCase(name)) {
                return definition;
            }
        }
        throw new IllegalArgumentException("未知指令冷却配置：" + name);
    }
}
