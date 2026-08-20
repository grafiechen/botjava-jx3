package com.grafie.botjava.observability;

import com.grafie.botjava.jx3.ws.Jx3ApiWebSocketStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Jx3ApiWebSocketHealthIndicatorTest {

    @Test
    void shouldBeDownWhenEnabledWebSocketIsDisconnected() {
        Jx3ApiWebSocketStatus status = mock(Jx3ApiWebSocketStatus.class);
        when(status.snapshot()).thenReturn(
                new Jx3ApiWebSocketStatus.Snapshot(true, false, "已断开，等待重连"));

        assertThat(new Jx3ApiWebSocketHealthIndicator(status).health().getStatus())
                .isEqualTo(Status.DOWN);
    }

    @Test
    void shouldExposeConnectionGauge() {
        Jx3ApiWebSocketStatus status = mock(Jx3ApiWebSocketStatus.class);
        when(status.snapshot()).thenReturn(
                new Jx3ApiWebSocketStatus.Snapshot(true, true, "已连接"));
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        new Jx3ApiWebSocketMetrics(registry, status);

        assertThat(registry.get("bot.jx3.websocket.connected").gauge().value()).isEqualTo(1D);
    }
    @Test
    void shouldRemainUpWhenWebSocketIsIntentionallyDisabled() {
        Jx3ApiWebSocketStatus status = mock(Jx3ApiWebSocketStatus.class);
        when(status.snapshot()).thenReturn(
                new Jx3ApiWebSocketStatus.Snapshot(false, false, "WS 未启用"));

        assertThat(new Jx3ApiWebSocketHealthIndicator(status).health().getStatus())
                .isEqualTo(Status.UP);
    }
}
