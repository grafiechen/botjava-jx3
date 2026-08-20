package com.grafie.botjava.jx3.ws;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Provides a credential-free snapshot of the effective JX3API WebSocket state.
 */
@Component
public class Jx3ApiWebSocketStatus {

    private final Environment environment;
    private final ObjectProvider<WebSocketClientInitializer> initializerProvider;

    public Jx3ApiWebSocketStatus(Environment environment,
                                 ObjectProvider<WebSocketClientInitializer> initializerProvider) {
        this.environment = environment;
        this.initializerProvider = initializerProvider;
    }

    public Snapshot snapshot() {
        boolean apiEnabled = environment.getProperty("jx3api.enabled", Boolean.class, true);
        boolean webSocketEnabled = environment.getProperty("jx3api.ws.enabled", Boolean.class, false);
        if (!apiEnabled) {
            return new Snapshot(false, false, "JX3API 未启用");
        }
        if (!webSocketEnabled) {
            return new Snapshot(false, false, "WS 未启用");
        }
        WebSocketClientInitializer initializer = initializerProvider.getIfAvailable();
        if (initializer == null) {
            return new Snapshot(true, false, "初始化中");
        }
        boolean connected = initializer.getConnectStatus();
        return new Snapshot(true, connected, connected ? "已连接" : "已断开，等待重连");
    }

    public record Snapshot(boolean enabled, boolean connected, String description) {
    }
}
