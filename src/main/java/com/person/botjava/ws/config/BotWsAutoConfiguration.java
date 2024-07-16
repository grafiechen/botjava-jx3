package com.person.botjava.ws.config;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author grafie.chen
 * @since 2024/7/16  15:06
 */
@Configuration
public class BotWsAutoConfiguration {
    @Resource
    private BotWSProperties botWSProperties;

    @Bean
    public BotWebSocketClientInitializer botWebSocketClientInitializer() {
        return new BotWebSocketClientInitializer(botWSProperties);
    }
}
