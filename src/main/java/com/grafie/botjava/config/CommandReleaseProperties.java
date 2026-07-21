package com.grafie.botjava.config;

import com.grafie.botjava.jx3.http.util.REGEX;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 指令发布阶段配置，用于隔离正式、测试和审核能力。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "bot.command")
public class CommandReleaseProperties {

    private RuntimeMode runtimeMode = RuntimeMode.PRODUCTION;

    public boolean allows(REGEX.CommandAvailability availability) {
        return switch (availability) {
            case SYSTEM, PRODUCTION -> true;
            case EXPERIMENTAL -> runtimeMode == RuntimeMode.TEST;
            case REVIEW -> runtimeMode == RuntimeMode.REVIEW;
        };
    }

    public enum RuntimeMode {
        PRODUCTION,
        TEST,
        REVIEW
    }
}
