package com.grafie.botjava.qq;

import com.grafie.botjava.entity.dto.qq.QqBotUserDto;
import com.grafie.botjava.observability.BotMetrics;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * QQ 当前机器人身份资源客户端。
 */
@Component
public class QqBotIdentityClient {

    private static final String CURRENT_BOT_PATH = "/users/@me";

    private final QqOpenApiClient openApiClient;
    private final BotMetrics botMetrics;

    public QqBotIdentityClient(QqOpenApiClient openApiClient, BotMetrics botMetrics) {
        this.openApiClient = openApiClient;
        this.botMetrics = botMetrics;
    }

    public QqBotUserDto me() {
        long startNanos = System.nanoTime();
        String outcome = "exception";
        try {
            QqBotUserDto bot = openApiClient.get(CURRENT_BOT_PATH, Map.of(), QqBotUserDto.class);
            if (bot == null || !hasText(bot.getId()) || !hasText(bot.getUsername())
                    || !Boolean.TRUE.equals(bot.getBot())) {
                outcome = "invalid_response";
                throw QqOpenApiErrorMapper.invalidResponse("QQ 机器人身份查询");
            }
            outcome = "success";
            return bot;
        } finally {
            botMetrics.recordQqRequest("bot_identity", outcome, System.nanoTime() - startNanos);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
