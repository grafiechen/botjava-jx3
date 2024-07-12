package com.person.botjava.ws.config;

import jx3api.api.ws.action.WsActionDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * webSocket服务类
 *
 * @author Grafie
 * @since 1.0.0
 */

public class BotWebSocketClientInitializer extends TextWebSocketHandler {
    private final Logger logger = LoggerFactory.getLogger(BotWebSocketClientInitializer.class);
    private BotWSProperties webSocketProperties;
    private BotWebSocketHandler webSocketHandler;
    private WebSocketConnectionManager webSocketConnectionManager;
    private IWsDataPushService iWsDataPushService;

    private WebSocketSession webSocketSession;
    private ScheduledExecutorService executorService;
    public BotWebSocketClientInitializer(BotWSProperties webSocketProperties, IWsDataPushService iWsDataPushService) throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        this.webSocketProperties = webSocketProperties;
        this.iWsDataPushService = iWsDataPushService;
        connect();
        // 初始化ws推送事件中，序列化相关信息
        initWsActionData(webSocketProperties);
    }

    private void initWsActionData(BotWSProperties webSocketProperties) throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        WsActionDataManager wsActionDataManager = new WsActionDataManager();
        wsActionDataManager.init(webSocketProperties.getWsDataBeanBasePackage());
    }

    private void connect() {
        beforeStartCheck();
        checkOnConnect();
    }

    private void beforeStartCheck() {
        checkProperties();
        checkWsHandler();
        checkConnectionManager();
    }

    private void checkConnectionManager() {
        if (this.webSocketConnectionManager != null) {
            return;
        }
        WebSocketClient webSocketClient = new StandardWebSocketClient();
        WebSocketConnectionManager connectionManager = new WebSocketConnectionManager(webSocketClient,
                this.webSocketHandler, webSocketProperties.getWsUrl());
        connectionManager.setAutoStartup(true);
        List<String> token = new ArrayList<>();
        token.add(webSocketProperties.getWsToken());
        connectionManager.getHeaders().put("token", token);
        this.webSocketConnectionManager = connectionManager;
    }

    private void checkWsHandler() {
        if (this.webSocketHandler == null) {
            this.webSocketHandler = new BotWebSocketHandler(this, iWsDataPushService);
        }
    }

    private void checkProperties() {
        if (webSocketProperties == null || webSocketProperties.getWsUrl() == null
                || webSocketProperties.getWsUrl().trim().length() <= 0) {
            throw new NullPointerException("webSocketProperties or wsUrl can not be null");
        }
    }

    /**
     * 异步校验连接状态，如果当前连接关闭，则尝试重连
     */
    @Async
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
                    logger.error("webSocket reConnect error，remote server url=>{}", webSocketProperties.getWsUrl(), e);
                }
                reConnectTime++;

            }
        }
    }

    /**
     * 获取连接状态
     *
     * @return true:connected
     */
    private boolean getConnectStatus() {
        synchronized (BotWebSocketClientInitializer.class) {
            return webSocketConnectionManager.isConnected();
        }
    }


    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        logger.info("WebSocket connect remote server success");
        this.webSocketSession = session;
        startPingTask();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        wsActionHandler.pushMessage(message);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        logger.error("WebSocket connection closed, try reConnect");
        webSocketClientInitializer.checkOnConnect();
    }

    @Override
    protected void handlePongMessage(WebSocketSession session, PongMessage message) throws Exception {
        super.handlePongMessage(session, message);
        logger.info("Received Pong message.");
    }

    /**
     * 发送ping消息
     *
     * @throws IOException exception
     */
    private void startPingTask() {
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(this::sendPingMessage, 0, 5, TimeUnit.SECONDS);
    }
    private void stopPingTask(){
        if (!executorService.isShutdown()){
            executorService.shutdown();
        }
    }
    private void sendPingMessage() {
        if (webSocketClientInitializer.getConnectStatus()) {
            try {
                this.webSocketSession.sendMessage(new PingMessage());
                logger.info("Sent Ping message, now time=>{}", TimeUtils.getNowString());
            } catch (Exception e) {
                logger.error("sent ping message error", e);
            }

        }
    }
}
