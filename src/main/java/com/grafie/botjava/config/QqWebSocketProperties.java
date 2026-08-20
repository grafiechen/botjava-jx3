package com.grafie.botjava.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * QQ OpenAPI v2 WebSocket 网关配置。
 */
@Component
@ConfigurationProperties(prefix = "bot.qq.websocket")
public class QqWebSocketProperties {

    /**
     * 群聊/C2C 相关事件 intent，当前群指令默认需要 1 << 25。
     */
    private int intents = 1 << 25;
    private int shardId = 0;
    private int shardCount = 1;
    private boolean useGatewayBot = true;
    private int reconnectDelaySeconds = 5;
    private int handshakeCheckSeconds = 10;

    public int getIntents() {
        return intents;
    }

    public void setIntents(int intents) {
        if (intents <= 0) {
            throw new IllegalArgumentException("bot.qq.websocket.intents 必须大于 0");
        }
        this.intents = intents;
    }

    public int getShardId() {
        return shardId;
    }

    public void setShardId(int shardId) {
        if (shardId < 0) {
            throw new IllegalArgumentException("bot.qq.websocket.shard-id 不能小于 0");
        }
        this.shardId = shardId;
    }

    public int getShardCount() {
        return shardCount;
    }

    public void setShardCount(int shardCount) {
        if (shardCount <= 0) {
            throw new IllegalArgumentException("bot.qq.websocket.shard-count 必须大于 0");
        }
        this.shardCount = shardCount;
    }

    public boolean isUseGatewayBot() {
        return useGatewayBot;
    }

    public void setUseGatewayBot(boolean useGatewayBot) {
        this.useGatewayBot = useGatewayBot;
    }

    public int getReconnectDelaySeconds() {
        return reconnectDelaySeconds;
    }

    public void setReconnectDelaySeconds(int reconnectDelaySeconds) {
        if (reconnectDelaySeconds < 1 || reconnectDelaySeconds > 300) {
            throw new IllegalArgumentException("bot.qq.websocket.reconnect-delay-seconds 必须在 1 到 300 之间");
        }
        this.reconnectDelaySeconds = reconnectDelaySeconds;
    }

    public int getHandshakeCheckSeconds() {
        return handshakeCheckSeconds;
    }

    public void setHandshakeCheckSeconds(int handshakeCheckSeconds) {
        if (handshakeCheckSeconds < 1 || handshakeCheckSeconds > 60) {
            throw new IllegalArgumentException("bot.qq.websocket.handshake-check-seconds 必须在 1 到 60 之间");
        }
        this.handshakeCheckSeconds = handshakeCheckSeconds;
    }
}
