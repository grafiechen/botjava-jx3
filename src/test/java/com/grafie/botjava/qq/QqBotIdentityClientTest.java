package com.grafie.botjava.qq;

import com.grafie.botjava.entity.dto.qq.QqBotUserDto;
import com.grafie.botjava.observability.BotMetrics;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QqBotIdentityClientTest {

    @Test
    void shouldLoadTypedCurrentBotIdentity() {
        QqOpenApiClient openApi = mock(QqOpenApiClient.class);
        BotMetrics metrics = mock(BotMetrics.class);
        QqBotUserDto bot = bot(true);
        when(openApi.get("/users/@me", Map.of(), QqBotUserDto.class)).thenReturn(bot);

        QqBotUserDto result = new QqBotIdentityClient(openApi, metrics).me();

        assertSame(bot, result);
        verify(metrics).recordQqRequest(eq("bot_identity"), eq("success"), anyLong());
    }

    @Test
    void shouldRejectIncompleteOrNonBotIdentityWithoutLeakingPayload() {
        QqOpenApiClient openApi = mock(QqOpenApiClient.class);
        BotMetrics metrics = mock(BotMetrics.class);
        when(openApi.get("/users/@me", Map.of(), QqBotUserDto.class)).thenReturn(bot(false));

        QqOpenApiException failure = assertThrows(QqOpenApiException.class,
                () -> new QqBotIdentityClient(openApi, metrics).me());

        assertEquals(QqOpenApiException.Category.INVALID_RESPONSE, failure.getCategory());
        verify(metrics).recordQqRequest(eq("bot_identity"), eq("invalid_response"), anyLong());
    }

    private static QqBotUserDto bot(boolean botAccount) {
        QqBotUserDto bot = new QqBotUserDto();
        bot.setId("bot-id");
        bot.setUsername("jx3-bot");
        bot.setBot(botAccount);
        return bot;
    }
}
