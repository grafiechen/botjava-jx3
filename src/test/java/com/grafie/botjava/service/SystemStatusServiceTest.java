package com.grafie.botjava.service;

import com.grafie.botjava.jx3.ws.Jx3ApiWebSocketStatus;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SystemStatusServiceTest {

    @Test
    void shouldRenderBothActuatorEndpointsAndConnectedWebSocket() {
        HealthEndpoint healthEndpoint = mock(HealthEndpoint.class);
        when(healthEndpoint.health()).thenReturn(Health.up().build());
        Jx3ApiWebSocketStatus webSocketStatus = mock(Jx3ApiWebSocketStatus.class);
        when(webSocketStatus.snapshot()).thenReturn(
                new Jx3ApiWebSocketStatus.Snapshot(true, true, "已连接"));
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

        String content = new SystemStatusService(healthEndpoint, registry, webSocketStatus).buildStatusText();

        assertThat(content)
                .contains("/actuator/health：可用（UP）")
                .contains("/actuator/prometheus：可用")
                .contains("JX3API WS：已连接");
    }

    @Test
    void shouldKeepEndpointFailuresIndependent() {
        HealthEndpoint healthEndpoint = mock(HealthEndpoint.class);
        when(healthEndpoint.health()).thenThrow(new IllegalStateException("health unavailable"));
        Jx3ApiWebSocketStatus webSocketStatus = mock(Jx3ApiWebSocketStatus.class);
        when(webSocketStatus.snapshot()).thenReturn(
                new Jx3ApiWebSocketStatus.Snapshot(true, false, "已断开，等待重连"));
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

        String content = new SystemStatusService(healthEndpoint, registry, webSocketStatus).buildStatusText();

        assertThat(content)
                .contains("/actuator/health：不可用")
                .contains("/actuator/prometheus：可用")
                .contains("JX3API WS：已断开，等待重连");
    }
}
