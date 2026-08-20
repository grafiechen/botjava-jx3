package com.grafie.botjava.jx3.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.jx3.ws.action.WsActionHandler;
import com.grafie.botjava.jx3.ws.service.WsDataPushService;
import com.grafie.botjava.util.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * JX3API WebSocket 消息与心跳处理器。
 */
public class CustomWebSocketHandler extends TextWebSocketHandler {

    private final Logger logger = LoggerFactory.getLogger(CustomWebSocketHandler.class);
    private final WebSocketClientInitializer webSocketClientInitializer;
    private final WsActionHandler wsActionHandler;

    private volatile WebSocketSession webSocketSession;
    private volatile ScheduledExecutorService pingExecutor;

    public CustomWebSocketHandler(WebSocketClientInitializer webSocketClientInitializer,
                                  WsDataPushService wsDataPushService,
                                  ObjectMapper objectMapper) {
        this.webSocketClientInitializer = webSocketClientInitializer;
        this.wsActionHandler = new WsActionHandler(wsDataPushService, objectMapper);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        logger.info("JX3API WebSocket 连接成功，remoteAddress=>{}", session.getRemoteAddress());
        webSocketSession = session;
        webSocketClientInitializer.markConnected();
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
        if (webSocketSession == session) {
            webSocketSession = null;
            stopPingTask();
        }
        if (webSocketClientInitializer.isShuttingDown()) {
            logger.info("JX3API WebSocket 因应用停止而断开，不再重连，remoteAddress=>{}，closeCode=>{}，reason=>{}",
                    session.getRemoteAddress(), status.getCode(), status.getReason());
            return;
        }
        logger.warn("JX3API WebSocket 连接已断开，remoteAddress=>{}，closeCode=>{}，reason=>{}，nextRetrySeconds=>{}",
                session.getRemoteAddress(), status.getCode(), status.getReason(),
                webSocketClientInitializer.getReconnectDelaySeconds());
        webSocketClientInitializer.checkOnConnect();
    }
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        if (webSocketClientInitializer.isShuttingDown()) {
            logger.debug("应用停止期间忽略 JX3API WebSocket 传输异常");
            return;
        }
        logger.error("JX3API WebSocket 传输异常，将按退避配置重连，remoteAddress=>{}，nextRetrySeconds=>{}",
                session == null ? null : session.getRemoteAddress(),
                webSocketClientInitializer.getReconnectDelaySeconds(), exception);
        closeQuietly(session);
        webSocketClientInitializer.checkOnConnect();
    }
    @Override
    protected void handlePongMessage(WebSocketSession session, PongMessage message) throws Exception {
        super.handlePongMessage(session, message);
        logger.debug("收到 JX3API WebSocket Pong");
    }

    private synchronized void startPingTask() {
        stopPingTask();
        pingExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "jx3api-websocket-ping");
            thread.setDaemon(true);
            return thread;
        });
        pingExecutor.scheduleAtFixedRate(this::sendPingMessage, 5, 5, TimeUnit.SECONDS);
    }

    private synchronized void stopPingTask() {
        ScheduledExecutorService executor = pingExecutor;
        pingExecutor = null;
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private void sendPingMessage() {
        WebSocketSession session = webSocketSession;
        if (!webSocketClientInitializer.getConnectStatus() || session == null || !session.isOpen()) {
            return;
        }
        try {
            session.sendMessage(new PingMessage());
            logger.debug("发送 JX3API WebSocket Ping，time=>{}", TimeUtils.getNowString());
        } catch (Exception e) {
            logger.error("发送 JX3API WebSocket Ping 失败，将按退避配置重连", e);
            closeQuietly(session);
            webSocketClientInitializer.checkOnConnect();
        }
    }

    private void closeQuietly(WebSocketSession session) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            session.close();
        } catch (IOException closeError) {
            logger.warn("关闭异常的 JX3API WebSocket session 失败", closeError);
        }
    }
}
