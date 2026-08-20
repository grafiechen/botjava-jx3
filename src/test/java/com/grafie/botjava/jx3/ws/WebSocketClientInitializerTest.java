package com.grafie.botjava.jx3.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.jx3.config.WebSocketProperties;
import com.grafie.botjava.jx3.ws.service.WsDataPushService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class WebSocketClientInitializerTest {

    @Test
    void shouldRejectReconnectDelayBelowThirtySeconds() {
        WebSocketProperties properties = properties(29);

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不能小于 30 秒");
    }

    @Test
    void shouldCoalesceReconnectRequestsAndScheduleAfterThirtySeconds() throws Exception {
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        WebSocketClientInitializer initializer = new WebSocketClientInitializer(
                properties(30), mock(WsDataPushService.class), new ObjectMapper(), executor, false);

        initializer.checkOnConnect();
        initializer.checkOnConnect();

        verify(executor, times(1)).schedule(any(Runnable.class), eq(30L), eq(TimeUnit.SECONDS));
        initializer.shutdown();
        verify(executor).shutdownNow();
    }

    private WebSocketProperties properties(int reconnectDelaySeconds) {
        WebSocketProperties properties = new WebSocketProperties();
        properties.setWsUrl("wss://socket.example.test");
        properties.setWsToken("test-token");
        properties.setReConnectMaxTimes(10);
        properties.setReConnectDelaySeconds(reconnectDelaySeconds);
        properties.setWsDataBeanBasePackage(new ArrayList<>());
        return properties;
    }
}
