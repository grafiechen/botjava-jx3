package com.grafie.botjava.jx3.http;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.cache.Jx3ApiResponseCache;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.observability.BotMetrics;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Jx3RequestUtilLoggingTest {

    @Test
    void shouldNotWriteRequestParametersOrCredentialsToInfoLog() {
        ApiProperties properties = new ApiProperties();
        properties.setApiUrl("https://example.invalid");
        properties.setApiToken("PRIVATE_TOKEN_MARKER");
        Jx3ApiResponseCache cache = mock(Jx3ApiResponseCache.class);
        RequestResult cached = new RequestResult();
        cached.setCode(200);
        cached.setMsg("success");
        when(cache.get(eq("/data/role/detail"), anyMap())).thenReturn(Optional.of(cached));

        Jx3RequestUtil requestUtil = new Jx3RequestUtil(
                properties, new ObjectMapper(), cache, mock(BotMetrics.class));
        Logger logger = (Logger) LoggerFactory.getLogger(Jx3RequestUtil.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            requestUtil.doPostRequest("/data/role/detail", Map.of(
                    "server", "PRIVATE_SERVER_MARKER",
                    "name", "PRIVATE_ROLE_MARKER"));
        }
        finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        String logText = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (left, right) -> left + "\n" + right);
        assertTrue(logText.contains("/data/role/detail"));
        assertFalse(logText.contains("PRIVATE_TOKEN_MARKER"));
        assertFalse(logText.contains("PRIVATE_SERVER_MARKER"));
        assertFalse(logText.contains("PRIVATE_ROLE_MARKER"));
    }
}
