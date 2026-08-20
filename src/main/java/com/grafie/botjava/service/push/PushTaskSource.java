package com.grafie.botjava.service.push;

public enum PushTaskSource {
    QQ_WS("QQ WebSocket"),
    JX3API_WS("JX3API WebSocket"),
    SCHEDULED("自定义定时任务");

    private final String displayName;

    PushTaskSource(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isWebSocket() {
        return this == QQ_WS || this == JX3API_WS;
    }
}