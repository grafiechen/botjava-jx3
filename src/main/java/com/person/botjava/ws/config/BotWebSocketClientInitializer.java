package com.person.botjava.ws.config;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.util.concurrent.ScheduledExecutorService;

/**
 * webSocket服务类
 *
 * @author Grafie
 * @since 1.0.0
 */

public class BotWebSocketClientInitializer {
    private final Logger logger = LoggerFactory.getLogger(BotWebSocketClientInitializer.class);
    private BotWSProperties webSocketProperties;
    private BotWebSocketHandler webSocketHandler;
    private WebSocketConnectionManager webSocketConnectionManager;

    private WebSocketSession webSocketSession;
    private ScheduledExecutorService executorService;

    public BotWebSocketClientInitializer(BotWSProperties webSocketProperties) {
        this.webSocketProperties = webSocketProperties;
        this.webSocketHandler = new BotWebSocketHandler(this);
        connect();
    }


    private void connect() {
        beforeStartCheck();
        checkOnConnect();
    }

    private void beforeStartCheck() {
        checkProperties();
        checkConnectionManager();
    }

    private void checkConnectionManager() {
        if (this.webSocketConnectionManager != null) {
            return;
        }
        WebSocketClient webSocketClient = new StandardWebSocketClient();
        WebSocketConnectionManager connectionManager = new WebSocketConnectionManager(webSocketClient,
                this.webSocketHandler, webSocketProperties.getUrl());
        connectionManager.setAutoStartup(true);
        this.webSocketConnectionManager = connectionManager;
    }


    private void checkProperties() {
        if (webSocketProperties == null) {
            throw new RuntimeException("websocket config properties can not be null");
        }
        if (StringUtils.isBlank(webSocketProperties.getUrl())) {
            throw new RuntimeException("websocket wsUrl can not be null");
        }
        if (StringUtils.isBlank(webSocketProperties.getToken())) {
            throw new RuntimeException("websocket token can not be null");
        }
        if (webSocketProperties.getShard() == null) {
            throw new RuntimeException("websocket shard can not be null");
        }
        if (webSocketProperties.getIntents() == null || webSocketProperties.getIntents() == 0) {
            throw new RuntimeException("websocket intents can not be null");
        }
    }

    /**
     * 校验连接状态，如果当前连接关闭，则尝试重连
     */
    public void checkOnConnect() {
        synchronized (BotWebSocketClientInitializer.class) {
            int reConnectTime = 0;
            while (reConnectTime < webSocketProperties.getReConnectMaxTimes()) {
                if (getConnectStatus()) {
                    return;
                }
                try {
                    beforeStartCheck();
                    webSocketConnectionManager.start();
                    if (getConnectStatus()) {
                        logger.info("webSocket reConnect success");
                        return;
                    }
                } catch (Exception e) {
                    logger.error("webSocket reConnect error，remote server url=>{}", webSocketProperties.getUrl(), e);
                }
                reConnectTime++;
            }
            logger.error("websocket reconnect times max,remote server url=>{}", webSocketProperties.getUrl());
        }
    }

    /**
     * 获取连接状态
     *
     * @return true:connected
     */
    public boolean getConnectStatus() {
        synchronized (BotWebSocketClientInitializer.class) {
            return webSocketConnectionManager.isConnected();
        }
    }

    /**
     * 获取配置信息
     *
     * @return BotWSProperties
     */
    public BotWSProperties getConfigProperties() {
        return webSocketProperties;
    }

}
