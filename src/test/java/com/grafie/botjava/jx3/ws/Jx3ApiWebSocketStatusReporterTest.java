package com.grafie.botjava.jx3.ws;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Jx3ApiWebSocketStatusReporterTest {

    @Test
    void shouldLogDisabledReasonWhenWebSocketIsOff() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("jx3api.enabled", "true")
                .withProperty("jx3api.ws.enabled", "false");

        String logs = captureLogs(() -> new Jx3ApiWebSocketStatusReporter(environment).reportStatus());

        assertTrue(logs.contains("JX3API WebSocket 未启用"));
        assertTrue(logs.contains("reason=>jx3api.ws.enabled=false"));
    }

    @Test
    void shouldLogEnabledSummaryWithoutTokenValue() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("jx3api.enabled", "true")
                .withProperty("jx3api.ws.enabled", "true")
                .withProperty("jx3api.ws.ws-url", "wss://socket.nicemoe.cn/private?token=query-secret")
                .withProperty("jx3api.ws.ws-token", "PRIVATE_WS_TOKEN")
                .withProperty("jx3api.ws.re-connect-delay-seconds", "30");

        String logs = captureLogs(() -> new Jx3ApiWebSocketStatusReporter(environment).reportStatus());

        assertTrue(logs.contains("JX3API WebSocket 已启用"));
        assertTrue(logs.contains("endpointHost=>socket.nicemoe.cn"));
        assertTrue(logs.contains("tokenConfigured=>true"));
        assertTrue(logs.contains("reconnectDelaySeconds=>30"));
        assertFalse(logs.contains("PRIVATE_WS_TOKEN"));
        assertFalse(logs.contains("query-secret"));
    }

    private String captureLogs(Runnable runnable) {
        Logger logger = (Logger) LoggerFactory.getLogger(Jx3ApiWebSocketStatusReporter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            runnable.run();
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
        return appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (left, right) -> left + "\n" + right);
    }
}