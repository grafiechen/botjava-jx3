package com.grafie.botjava.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.config.QqIngressProperties;
import com.grafie.botjava.config.QqWebSocketProperties;
import com.grafie.botjava.contants.OpCode;
import com.grafie.botjava.contants.PayloadTEnum;
import com.grafie.botjava.entity.dto.payload.Payload;
import com.grafie.botjava.entity.dto.qq.QqGatewayDto;
import com.grafie.botjava.observability.RequestTraceContext;
import com.grafie.botjava.qq.QqOpenApiClient;
import com.grafie.botjava.qq.QqOpenApiException;
import com.grafie.botjava.util.SensitiveDataUtil;
import com.grafie.botjava.util.OutboundHttpRateLimiter;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * QQ OpenAPI v2 WebSocket 入站服务。
 * <p>
 * 只负责连接、鉴权、心跳和事件转发；业务事件仍复用 BotMessageService。
 */
@Slf4j
@Service
public class QqWebSocketGatewayService {

    private static final long DEFAULT_HEARTBEAT_INTERVAL_MILLIS = 45_000L;

    private final QqIngressProperties ingressProperties;
    private final QqWebSocketProperties webSocketProperties;
    private final QqGatewayClient gatewayClient;
    private final QqOpenApiClient openApiClient;
    private final BotMessageService botMessageService;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "qq-websocket-gateway");
        thread.setDaemon(true);
        return thread;
    });

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong connectionGeneration = new AtomicLong();
    private volatile ScheduledFuture<?> reconnectTask;
    private volatile WebSocketConnectionManager connectionManager;
    private volatile WebSocketSession currentSession;
    private volatile ScheduledFuture<?> heartbeatTask;
    private volatile Integer latestSequence;
    private volatile String sessionId;

    public QqWebSocketGatewayService(QqIngressProperties ingressProperties,
                                     QqWebSocketProperties webSocketProperties,
                                     QqGatewayClient gatewayClient,
                                     QqOpenApiClient openApiClient,
                                     BotMessageService botMessageService,
                                     ObjectMapper objectMapper) {
        this.ingressProperties = ingressProperties;
        this.webSocketProperties = webSocketProperties;
        this.gatewayClient = gatewayClient;
        this.openApiClient = openApiClient;
        this.botMessageService = botMessageService;
        this.objectMapper = objectMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startOnApplicationReady() {
        if (!ingressProperties.isWebSocketEnabled()) {
            log.info("QQ WebSocket 入站未启用，messageIngressMode=>{}", ingressProperties.getMessageIngressMode());
            return;
        }
        if (running.compareAndSet(false, true)) {
            log.info("QQ WebSocket 入站已启用，intents=>{}，shard=>[{}/{}]",
                    webSocketProperties.getIntents(), webSocketProperties.getShardId(),
                    webSocketProperties.getShardCount());
            scheduleConnect(Duration.ZERO);
        }
    }

    @PreDestroy
    public void shutdown() {
        running.set(false);
        cancelHeartbeat();
        WebSocketConnectionManager manager = connectionManager;
        if (manager != null) {
            manager.stop();
        }
        executor.shutdownNow();
    }

    void connectNowForTest() {
        running.set(true);
        connect();
    }

    Map<String, Object> buildIdentifyFrame() {
        return Map.of(
                "op", OpCode.IDENTIFY.getCode(),
                "d", Map.of(
                        "token", openApiClient.getAuthorizationValue(),
                        "intents", webSocketProperties.getIntents(),
                        "shard", List.of(webSocketProperties.getShardId(), webSocketProperties.getShardCount())
                )
        );
    }

    Map<String, Object> buildResumeFrame() {
        return Map.of(
                "op", OpCode.RESUME.getCode(),
                "d", Map.of(
                        "token", openApiClient.getAuthorizationValue(),
                        "session_id", sessionId,
                        "seq", latestSequence
                )
        );
    }

    Map<String, Object> buildHeartbeatFrame() {
        Map<String, Object> frame = new LinkedHashMap<>();
        frame.put("op", OpCode.HEARTBEAT.getCode());
        frame.put("d", latestSequence);
        return frame;
    }

    private synchronized void scheduleConnect(Duration delay) {
        if (!running.get()) {
            return;
        }
        ScheduledFuture<?> scheduled = reconnectTask;
        if (scheduled != null && !scheduled.isDone()) {
            return;
        }
        reconnectTask = executor.schedule(() -> {
            synchronized (QqWebSocketGatewayService.this) {
                reconnectTask = null;
            }
            connectSafely();
        }, Math.max(0, delay.toMillis()), TimeUnit.MILLISECONDS);
    }

    private void connectSafely() {
        try {
            connect();
        } catch (Exception e) {
            Duration delay = reconnectDelay(e);
            log.error("QQ WebSocket 连接失败，delaySeconds=>{}，reason=>{}",
                    delay.toSeconds(), SensitiveDataUtil.summarize(e), e);
            scheduleReconnect(connectionGeneration.get(), delay);
        }
    }

    private synchronized void connect() {
        if (!running.get()) {
            return;
        }
        long generation = connectionGeneration.incrementAndGet();
        stopConnectionManager();
        QqGatewayDto gateway = webSocketProperties.isUseGatewayBot()
                ? gatewayClient.getGatewayBot() : gatewayClient.getGateway();
        if (gateway == null || gateway.getUrl() == null || gateway.getUrl().isBlank()) {
            throw new IllegalStateException("QQ gateway 接口未返回有效 WebSocket URL");
        }
        String gatewayUrl = gateway.getUrl().trim();
        QqGatewayWebSocketHandler handler = new QqGatewayWebSocketHandler(generation);
        WebSocketConnectionManager manager = new WebSocketConnectionManager(
                new StandardWebSocketClient(), handler, gatewayUrl);
        manager.setAutoStartup(false);
        this.connectionManager = manager;
        log.info("QQ WebSocket 开始连接，generation=>{}，urlHost=>{}，useGatewayBot=>{}，recommendedShards=>{}",
                generation, safeHost(gatewayUrl), webSocketProperties.isUseGatewayBot(), gateway.getShards());
        OutboundHttpRateLimiter.awaitPermit();
        manager.start();
        executor.schedule(() -> verifyHandshake(manager, generation),
                webSocketProperties.getHandshakeCheckSeconds(), TimeUnit.SECONDS);
    }

    private void verifyHandshake(WebSocketConnectionManager manager, long generation) {
        if (!isCurrentGeneration(generation) || manager != connectionManager || manager.isConnected()) {
            return;
        }
        log.warn("QQ WebSocket 握手未在预期时间内完成，generation=>{}，准备重连，checkSeconds=>{}",
                generation, webSocketProperties.getHandshakeCheckSeconds());
        scheduleReconnect(generation, Duration.ofSeconds(webSocketProperties.getReconnectDelaySeconds()));
    }

    private synchronized void scheduleReconnect(long sourceGeneration, Duration delay) {
        if (!running.get() || !isCurrentGeneration(sourceGeneration)) {
            return;
        }
        connectionGeneration.incrementAndGet();
        stopConnectionManager();
        scheduleConnect(delay);
    }

    private Duration reconnectDelay(Exception failure) {
        if (failure instanceof QqOpenApiException qqFailure
                && qqFailure.getCategory() == QqOpenApiException.Category.RATE_LIMIT) {
            return Duration.ofSeconds(60);
        }
        return Duration.ofSeconds(webSocketProperties.getReconnectDelaySeconds());
    }

    private synchronized void stopConnectionManager() {
        cancelHeartbeat();
        currentSession = null;
        WebSocketConnectionManager manager = connectionManager;
        connectionManager = null;
        if (manager != null) {
            manager.stop();
        }
    }

    private synchronized void cancelReconnect() {
        ScheduledFuture<?> task = reconnectTask;
        reconnectTask = null;
        if (task != null) {
            task.cancel(true);
        }
    }

    private boolean isCurrentGeneration(long generation) {
        return running.get() && connectionGeneration.get() == generation;
    }
    private void handlePayload(WebSocketSession session, Payload payload, long generation) throws IOException {
        if (!isCurrentGeneration(generation) || session != currentSession) {
            return;
        }
        if (payload == null || payload.getOp() == null) {
            log.warn("QQ WebSocket 收到空 payload 或缺少 op");
            return;
        }
        OpCode opCode = OpCode.getByCode(payload.getOp());
        if (opCode == null) {
            log.warn("QQ WebSocket 收到未知 op，op=>{}，t=>{}", payload.getOp(), payload.getT());
            return;
        }
        switch (opCode) {
            case HELLO -> handleHello(session, payload, generation);
            case HEARTBEAT -> sendFrame(session, buildHeartbeatFrame());
            case HEARTBEAT_ACK -> log.debug("QQ WebSocket 心跳确认，sequence=>{}", latestSequence);
            case DISPATCH -> handleDispatch(payload);
            case RECONNECT -> {
                log.warn("QQ WebSocket 收到重连要求，sequence=>{}", latestSequence);
                scheduleReconnect(generation, Duration.ofSeconds(webSocketProperties.getReconnectDelaySeconds()));
            }
            case INVALID_SESSION -> {
                log.warn("QQ WebSocket 会话无效，清理 session 后重连，sequence=>{}", latestSequence);
                sessionId = null;
                latestSequence = null;
                scheduleReconnect(generation, Duration.ofSeconds(webSocketProperties.getReconnectDelaySeconds()));
            }
            default -> log.debug("QQ WebSocket 收到暂不需要处理的 op，op=>{}，t=>{}", payload.getOp(), payload.getT());
        }
    }

    private void handleHello(WebSocketSession session, Payload payload, long generation) throws IOException {
        long heartbeatInterval = heartbeatInterval(payload);
        startHeartbeat(session, heartbeatInterval, generation);
        if (sessionId != null && latestSequence != null) {
            sendFrame(session, buildResumeFrame());
            log.info("QQ WebSocket 已发送 Resume，sessionIdPresent=>true，sequence=>{}", latestSequence);
        } else {
            sendFrame(session, buildIdentifyFrame());
            log.info("QQ WebSocket 已发送 Identify，intents=>{}，shard=>[{}/{}]",
                    webSocketProperties.getIntents(), webSocketProperties.getShardId(),
                    webSocketProperties.getShardCount());
        }
    }

    private long heartbeatInterval(Payload payload) {
        JsonNode data = objectMapper.valueToTree(payload.getD());
        long interval = data.path("heartbeat_interval").asLong(DEFAULT_HEARTBEAT_INTERVAL_MILLIS);
        if (interval <= 0) {
            return DEFAULT_HEARTBEAT_INTERVAL_MILLIS;
        }
        return interval;
    }

    private void startHeartbeat(WebSocketSession session, long heartbeatIntervalMillis, long generation) {
        cancelHeartbeat();
        heartbeatTask = executor.scheduleAtFixedRate(
                () -> sendHeartbeatSafely(session, generation),
                heartbeatIntervalMillis, heartbeatIntervalMillis, TimeUnit.MILLISECONDS);
    }

    private void sendHeartbeatSafely(WebSocketSession session, long generation) {
        if (!isCurrentGeneration(generation) || session != currentSession) {
            return;
        }
        if (!session.isOpen()) {
            log.warn("QQ WebSocket 当前会话已关闭，generation=>{}，准备重连", generation);
            scheduleReconnect(generation, Duration.ofSeconds(webSocketProperties.getReconnectDelaySeconds()));
            return;
        }
        try {
            sendFrame(session, buildHeartbeatFrame());
        } catch (Exception e) {
            log.warn("QQ WebSocket 心跳发送失败，generation=>{}，reason=>{}",
                    generation, SensitiveDataUtil.summarize(e));
            scheduleReconnect(generation, Duration.ofSeconds(webSocketProperties.getReconnectDelaySeconds()));
        }
    }
    private void cancelHeartbeat() {
        ScheduledFuture<?> task = heartbeatTask;
        if (task != null) {
            task.cancel(true);
        }
        heartbeatTask = null;
    }

    private void handleDispatch(Payload payload) {
        latestSequence = payload.getS();
        if ("READY".equals(payload.getT())) {
            updateSessionId(payload.getD());
            log.info("QQ WebSocket Ready，sessionIdPresent=>{}，sequence=>{}",
                    sessionId != null, latestSequence);
            return;
        }
        if (PayloadTEnum.getByValue(payload.getT()) == null) {
            log.info("QQ WebSocket 忽略未注册 dispatch，t=>{}，sequence=>{}", payload.getT(), payload.getS());
            return;
        }
        String invocationId = "ws-" + (payload.getS() == null ? UUID.randomUUID() : payload.getS());
        try (RequestTraceContext.Scope ignored = RequestTraceContext.open(invocationId)) {
            botMessageService.dealMessage(payload);
        } catch (Exception e) {
            log.error("QQ WebSocket dispatch 处理失败，t=>{}，sequence=>{}，reason=>{}",
                    payload.getT(), payload.getS(), SensitiveDataUtil.summarize(e), e);
        }
    }

    private void updateSessionId(Object data) {
        JsonNode node = objectMapper.valueToTree(data);
        String value = node.path("session_id").asText(null);
        if (value != null && !value.isBlank()) {
            sessionId = value;
        }
    }

    private void sendFrame(WebSocketSession session, Map<String, Object> frame) throws IOException {
        if (session == null || !session.isOpen()) {
            throw new IOException("QQ WebSocket session 未连接");
        }
        synchronized (session) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(frame)));
        }
    }

    private String safeHost(String url) {
        try {
            return java.net.URI.create(url).getHost();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private final class QqGatewayWebSocketHandler extends TextWebSocketHandler {
        private final long generation;

        private QqGatewayWebSocketHandler(long generation) {
            this.generation = generation;
        }

        @Override
        public void afterConnectionEstablished(WebSocketSession session) throws Exception {
            if (!isCurrentGeneration(generation)) {
                session.close();
                return;
            }
            currentSession = session;
            log.info("QQ WebSocket 已建立连接，generation=>{}，sessionId=>{}", generation, session.getId());
        }

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
            if (!isCurrentGeneration(generation) || session != currentSession) {
                return;
            }
            Payload payload = objectMapper.readValue(message.getPayload(), Payload.class);
            handlePayload(session, payload, generation);
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) {
            if (!isCurrentGeneration(generation)) {
                log.debug("QQ WebSocket 忽略旧连接传输异常，generation=>{}", generation);
                return;
            }
            log.error("QQ WebSocket 传输异常，generation=>{}，sessionId=>{}，reason=>{}",
                    generation, session == null ? null : session.getId(),
                    SensitiveDataUtil.summarize(exception), exception);
            scheduleReconnect(generation, Duration.ofSeconds(webSocketProperties.getReconnectDelaySeconds()));
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            if (!isCurrentGeneration(generation)) {
                log.debug("QQ WebSocket 忽略旧连接关闭回调，generation=>{}，code=>{}", generation, status.getCode());
                return;
            }
            cancelHeartbeat();
            if (currentSession == session) {
                currentSession = null;
            }
            log.warn("QQ WebSocket 已关闭，generation=>{}，sessionId=>{}，code=>{}，reason=>{}",
                    generation, session == null ? null : session.getId(), status.getCode(), status.getReason());
            scheduleReconnect(generation, Duration.ofSeconds(webSocketProperties.getReconnectDelaySeconds()));
        }
    }
}
