package com.grafie.botjava.observability;

import com.grafie.botjava.jx3.ws.Jx3ApiWebSocketStatus;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class Jx3ApiWebSocketMetrics {

    public Jx3ApiWebSocketMetrics(MeterRegistry meterRegistry, Jx3ApiWebSocketStatus webSocketStatus) {
        Gauge.builder("bot.jx3.websocket.connected", webSocketStatus,
                        status -> status.snapshot().connected() ? 1D : 0D)
                .description("Whether the JX3API WebSocket connection is established")
                .register(meterRegistry);
    }
}
