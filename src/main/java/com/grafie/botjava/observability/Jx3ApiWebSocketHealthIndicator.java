package com.grafie.botjava.observability;

import com.grafie.botjava.jx3.ws.Jx3ApiWebSocketStatus;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("jx3ApiWebSocket")
public class Jx3ApiWebSocketHealthIndicator implements HealthIndicator {

    private final Jx3ApiWebSocketStatus webSocketStatus;

    public Jx3ApiWebSocketHealthIndicator(Jx3ApiWebSocketStatus webSocketStatus) {
        this.webSocketStatus = webSocketStatus;
    }

    @Override
    public Health health() {
        Jx3ApiWebSocketStatus.Snapshot snapshot = webSocketStatus.snapshot();
        if (!snapshot.enabled()) {
            return Health.up()
                    .withDetail("enabled", false)
                    .withDetail("state", snapshot.description())
                    .build();
        }
        Health.Builder builder = snapshot.connected() ? Health.up() : Health.down();
        return builder
                .withDetail("enabled", true)
                .withDetail("state", snapshot.description())
                .build();
    }
}
