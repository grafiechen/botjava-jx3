package com.grafie.botjava.util;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author grafie.chen
 * @since 2025/1/22  15:00
 */
@Component
public class BotPropertyUtil {
    private static String botSecret;
    @Value("${my.bot.secret}")
    private String initSecret;

    @PostConstruct
    public void init() {
        botSecret = initSecret;
    }

    /**
     * 获取bot secret
     *
     * @return String
     */
    public static String getBotSecret() {
        return botSecret;
    }
}
