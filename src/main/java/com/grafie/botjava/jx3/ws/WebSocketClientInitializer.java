package com.grafie.botjava.jx3.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.jx3.config.WebSocketProperties;
import com.grafie.botjava.jx3.ws.action.WsActionDataManager;
import com.grafie.botjava.jx3.ws.service.WsDataPushService;
import com.grafie.botjava.util.OutboundHttpRateLimiter;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JX3API WebSocket 连接管理器。
 */
public class WebSocketClientInitializer {

    private final Logger logger = LoggerFactory.getLogger(WebSocketClientInitializer.class);
    private final WebSocketProperties webSocketProperties;
    private final WsDataPushService wsDataPushService;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService reconnectExecutor;
    private final AtomicBoolean connectionAttemptScheduled = new AtomicBoolean(false);
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);

    private volatile CustomWebSocketHandler webSocketHandler;
    private volatile WebSocketConnectionManager webSocketConnectionManager;
    private volatile boolean shuttingDown;

    public WebSocketClientInitializer(WebSocketProperties webSocketProperties,
                                      WsDataPushService wsDataPushService,
                                      ObjectMapper objectMapper)
            throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException,
            InstantiationException, IllegalAccessException {
        this(webSocketProperties, wsDataPushService, objectMapper, newReconnectExecutor(), true);
    }

    WebSocketClientInitializer(WebSocketProperties webSocketProperties,
                               WsDataPushService wsDataPushService,
                               ObjectMapper objectMapper,
                               ScheduledExecutorService reconnectExecutor,
                               boolean connectOnStartup)
            throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException,
            InstantiationException, IllegalAccessException {
        this.webSocketProperties = Objects.requireNonNull(webSocketProperties, "webSocketProperties");
        this.wsDataPushService = Objects.requireNonNull(wsDataPushService, "wsDataPushService");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.reconnectExecutor = Objects.requireNonNull(reconnectExecutor, "reconnectExecutor");
        this.webSocketProperties.validate();
        initWsActionData(webSocketProperties);
        beforeStartCheck();
        if (connectOnStartup) {
            scheduleConnection(0, "startup");
        }
    }

    private static ScheduledExecutorService newReconnectExecutor() {
        return Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "jx3api-websocket-reconnect");
            thread.setDaemon(true);
            return thread;
        });
    }

    private void initWsActionData(WebSocketProperties properties)
            throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException,
            InstantiationException, IllegalAccessException {
        WsActionDataManager wsActionDataManager = new WsActionDataManager();
        wsActionDataManager.init(properties.getWsDataBeanBasePackage());
    }

    private void beforeStartCheck() {
        checkProperties();
        checkWsHandler();
        checkConnectionManager();
    }

    private synchronized void checkConnectionManager() {
        if (webSocketConnectionManager != null) {
            return;
        }
        WebSocketClient webSocketClient = new StandardWebSocketClient();
        WebSocketConnectionManager connectionManager = new WebSocketConnectionManager(
                webSocketClient, webSocketHandler, webSocketProperties.getWsUrl());
        connectionManager.setAutoStartup(false);
        List<String> token = new ArrayList<>();
        token.add(webSocketProperties.getWsToken());
        connectionManager.getHeaders().put("token", token);
        webSocketConnectionManager = connectionManager;
    }

    private synchronized void checkWsHandler() {
        if (webSocketHandler == null) {
            webSocketHandler = new CustomWebSocketHandler(this, wsDataPushService, objectMapper);
        }
    }

    private void checkProperties() {
        webSocketProperties.validate();
    }

    /**
     * 断线回调入口。重复调用会合并成同一个计划任务。
     */
    public void checkOnConnect() {
        scheduleConnection(webSocketProperties.getReConnectDelaySeconds(), "connection-lost");
    }

    void markConnected() {
        int completedAttempt = reconnectAttempts.getAndSet(0);
        logger.info("JX3API WebSocket 连接已确认，remoteServerUrl=>{}，attempt=>{}，重试计数已重置",
                webSocketProperties.getWsUrl(), completedAttempt);
    }

    boolean isShuttingDown() {
        return shuttingDown;
    }

    int getReconnectDelaySeconds() {
        return Math.max(30, webSocketProperties.getReConnectDelaySeconds());
    }
    private void scheduleConnection(long delaySeconds, String reason) {
        if (shuttingDown || getConnectStatus()) {
            return;
        }
        if (reconnectAttempts.get() >= webSocketProperties.getReConnectMaxTimes()) {
            logger.error("JX3API WebSocket 已达到最大重试次数，remoteServerUrl=>{}，maxAttempts=>{}",
                    webSocketProperties.getWsUrl(), webSocketProperties.getReConnectMaxTimes());
            return;
        }
        if (!connectionAttemptScheduled.compareAndSet(false, true)) {
            logger.debug("JX3API WebSocket 重连任务已存在，忽略重复调度，reason=>{}", reason);
            return;
        }
        long safeDelaySeconds = delaySeconds <= 0
                ? 0
                : Math.max(30, delaySeconds);
        logger.info("JX3API WebSocket 连接任务已调度，reason=>{}，delaySeconds=>{}，attempt=>{}/{}",
                reason, safeDelaySeconds, reconnectAttempts.get() + 1,
                webSocketProperties.getReConnectMaxTimes());
        try {
            reconnectExecutor.schedule(this::executeConnectionAttempt,
                    safeDelaySeconds, TimeUnit.SECONDS);
        } catch (RuntimeException e) {
            connectionAttemptScheduled.set(false);
            if (!shuttingDown) {
                logger.error("JX3API WebSocket 重连任务调度失败，reason=>{}", reason, e);
            }
        }
    }

    private void executeConnectionAttempt() {
        try {
            if (shuttingDown || getConnectStatus()) {
                return;
            }
            int attempt = reconnectAttempts.incrementAndGet();
            beforeStartCheck();
            WebSocketConnectionManager manager = webSocketConnectionManager;
            if (manager.isRunning()) {
                manager.stop();
            }
            OutboundHttpRateLimiter.awaitPermit();
            logger.info("开始连接 JX3API WebSocket，remoteServerUrl=>{}，attempt=>{}/{}",
                    webSocketProperties.getWsUrl(), attempt,
                    webSocketProperties.getReConnectMaxTimes());
            manager.start();
        } catch (Exception e) {
            logger.error("JX3API WebSocket 连接失败，remoteServerUrl=>{}，attempt=>{}",
                    webSocketProperties.getWsUrl(), reconnectAttempts.get(), e);
        } finally {
            connectionAttemptScheduled.set(false);
            if (!shuttingDown && !getConnectStatus()
                    && reconnectAttempts.get() < webSocketProperties.getReConnectMaxTimes()) {
                scheduleConnection(webSocketProperties.getReConnectDelaySeconds(), "connect-not-ready");
            }
        }
    }

    public boolean getConnectStatus() {
        WebSocketConnectionManager manager = webSocketConnectionManager;
        return manager != null && manager.isConnected();
    }

    @PreDestroy
    public void shutdown() {
        boolean connected = getConnectStatus();
        boolean reconnectPending = connectionAttemptScheduled.get();
        logger.info("开始停止 JX3API WebSocket，remoteServerUrl=>{}，connected=>{}，reconnectPending=>{}",
                webSocketProperties.getWsUrl(), connected, reconnectPending);
        shuttingDown = true;
        connectionAttemptScheduled.set(false);
        reconnectExecutor.shutdownNow();
        WebSocketConnectionManager manager = webSocketConnectionManager;
        if (manager != null && manager.isRunning()) {
            try {
                manager.stop();
            } catch (RuntimeException e) {
                logger.warn("停止 JX3API WebSocket 连接失败", e);
            }
        }
        logger.info("JX3API WebSocket 已停止，remoteServerUrl=>{}", webSocketProperties.getWsUrl());
    }
}
