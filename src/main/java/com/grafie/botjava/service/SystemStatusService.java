package com.grafie.botjava.service;

import com.grafie.botjava.jx3.ws.Jx3ApiWebSocketStatus;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.stereotype.Service;

@Service
public class SystemStatusService {

    private final HealthEndpoint healthEndpoint;
    private final PrometheusMeterRegistry prometheusMeterRegistry;
    private final Jx3ApiWebSocketStatus webSocketStatus;

    public SystemStatusService(HealthEndpoint healthEndpoint,
                               PrometheusMeterRegistry prometheusMeterRegistry,
                               Jx3ApiWebSocketStatus webSocketStatus) {
        this.healthEndpoint = healthEndpoint;
        this.prometheusMeterRegistry = prometheusMeterRegistry;
        this.webSocketStatus = webSocketStatus;
    }

    public String buildStatusText() {
        String healthStatus = readHealthStatus();
        String prometheusStatus = readPrometheusStatus();
        Jx3ApiWebSocketStatus.Snapshot webSocket = webSocketStatus.snapshot();
        return "系统状态"
                + "\n/actuator/health：" + healthStatus
                + "\n/actuator/prometheus：" + prometheusStatus
                + "\nJX3API WS：" + webSocket.description();
    }

    private String readHealthStatus() {
        try {
            return "可用（" + healthEndpoint.health().getStatus().getCode() + "）";
        } catch (RuntimeException exception) {
            return "不可用";
        }
    }

    private String readPrometheusStatus() {
        try {
            return prometheusMeterRegistry.scrape() == null ? "不可用" : "可用";
        } catch (RuntimeException exception) {
            return "不可用";
        }
    }
}
