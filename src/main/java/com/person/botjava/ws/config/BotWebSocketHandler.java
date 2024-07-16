package com.person.botjava.ws.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.person.botjava.util.TimeUtils;
import com.person.botjava.ws.contants.Opcode;
import com.person.botjava.ws.dto.IdentifyDto;
import com.person.botjava.ws.dto.Payload;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ws 处理器
 *
 * @author Grafie
 * @since 1.0.0
 */

public class BotWebSocketHandler extends TextWebSocketHandler {
    private final Logger logger = LoggerFactory.getLogger(BotWebSocketHandler.class);
    private BotWebSocketClientInitializer webSocketClientInitializer;
    private WebSocketSession webSocketSession;
    private ScheduledExecutorService executorService;
    private BotWSProperties botWSProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static boolean startedHeartBeat = false;
    /**
     * 接收事件时候的s字段，在resume的时候，将s字段的值，当作seq传递回去
     */
    private static Integer seq = null;

    public BotWebSocketHandler(BotWebSocketClientInitializer webSocketClientInitializer) {
        this.webSocketClientInitializer = webSocketClientInitializer;
        this.botWSProperties = webSocketClientInitializer.getConfigProperties();
    }


    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        logger.info("WebSocket connect remote server success");
        this.webSocketSession = session;
        sendIdentifyMessage();
    }

    private void sendIdentifyMessage() {
        IdentifyDto identifyDto = new IdentifyDto();
        identifyDto.setToken(botWSProperties.getToken());
        identifyDto.setShard(botWSProperties.getShard());
        identifyDto.setIntents(botWSProperties.getIntents());
        if (StringUtils.isNotBlank(botWSProperties.getProperties())) {
            try {
                Map<String, String> map = objectMapper.readValue(botWSProperties.getProperties(),
                        new TypeReference<Map<String, String>>() {
                        });
                identifyDto.setProperties(map);
            } catch (Exception e) {
                logger.error("transform identify properties error,properties=>{}", botWSProperties, e);
            }
        }
        Payload payload = new Payload();
        payload.setOp(Opcode.IDENTIFY.getCode());
        payload.setD(identifyDto);
        sendMessage(payload);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        logger.info("receive message =>{}", message.getPayload());
        // TODO 后续优化，目前只有几个不同的类型，现在这个类里面处理，后面多了再说
        handle(message.getPayload());
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
    private void startHeartBeatTask(Integer time) {
        if (startedHeartBeat) {
            return;
        }
        startedHeartBeat = true;
        executorService = Executors.newSingleThreadScheduledExecutor();
        int period = (int) (time * 0.85);
        // 取超时时间的0.85作为心跳间隔，避免因为各种原因造成的断联
        executorService.scheduleAtFixedRate(this::sendHeartBeatMessage, 0, period, TimeUnit.MILLISECONDS);
    }

    private void sendHeartBeatMessage() {
        if (webSocketClientInitializer.getConnectStatus()) {
            try {
                Payload payload = new Payload();
                payload.setOp(Opcode.HEARTBEAT.getCode());
                payload.setD(seq);
                sendMessage(payload);
                logger.info("Sent Ping message, now time=>{}", TimeUtils.getNowString());
            } catch (Exception e) {
                logger.error("sent ping message error", e);
            }

        }
    }

    private void sendMessage(Payload payload) {
        String message = null;
        try {
            message = objectMapper.writeValueAsString(payload);
            logger.info("send message =>{}", message);
            TextMessage textMessage = new TextMessage(message);
            webSocketSession.sendMessage(textMessage);
        } catch (JsonProcessingException jsE) {
            logger.error("websocket sendMessage, parse json error, value=>{}", payload, jsE);
        } catch (IOException ioe) {
            logger.error("websocket sendMessage error,value=>{}, message=>{}", payload, message, ioe);
        }

    }

    private void handle(String message) {
        try {
            Payload payload = objectMapper.readValue(message, Payload.class);
            seq = payload.getS();
            int op = payload.getOp();
            Opcode opcodeEnum = Opcode.getByCode(op);
            // 只需要实现receive 类型的即可
            switch (opcodeEnum) {
                case DISPATCH:
                    dispatchMessage(payload);
                    break;
                case HEARTBEAT:
                    heartBeat(payload);
                    break;
                case RECONNECT:
                    reconnectMessage(payload);
                    break;
                case INVALID_SESSION:
                    invalidSession(message, payload);
                    break;
                case HELLO:
                    helloMessage(payload);
                    break;
                case HTTP_CALLBACK_ACK:
                    httpCallBackAck(payload);
                    break;
                default:
                    break;
            }
        } catch (JsonProcessingException e) {
            logger.error("处理消息时，序列化消息内容出错, message=>{}", message, e);
        }

    }

    private void httpCallBackAck(Payload payload) {
        // 没啥需要处理的
        logger.info("收到http回调确认消息,message=>{}", payload);
    }

    private void helloMessage(Payload payload) {
        logger.info("连接成功，接收到hello消息=>{}", payload);
        LinkedHashMap<String, Integer> map = (LinkedHashMap<String, Integer>) payload.getD();
        startHeartBeatTask(map.get("heartbeat_interval"));
    }

    private void reconnectMessage(Payload payload) {
    }

    private void heartBeat(Payload payload) {
        logger.info("接收到心跳消息，message=>{}", payload);
    }

    private void dispatchMessage(Payload payload) {
    }

    private void invalidSession(String message, Payload payload) {
        logger.error("服务端返回鉴权失败，请检查参数,source message =>{},result=>{}", message, payload);
        throw new RuntimeException("服务端返回参数校验错误");
    }
}
