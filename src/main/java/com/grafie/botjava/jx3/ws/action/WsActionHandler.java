package com.grafie.botjava.jx3.ws.action;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.jx3.ws.data.BaseWsData;
import com.grafie.botjava.jx3.ws.data.GenericWsData;
import com.grafie.botjava.jx3.ws.data.Jx3WsEventEnum;
import com.grafie.botjava.jx3.ws.service.WsDataPushService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * JX3API WebSocket 事件解析与分发。
 */
public class WsActionHandler {
    private static final Logger logger = LoggerFactory.getLogger(WsActionHandler.class);

    private final WsDataPushService wsDataPushService;
    private final ObjectMapper objectMapper;

    public WsActionHandler(WsDataPushService wsDataPushService, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.wsDataPushService = wsDataPushService;
    }

    public void pushMessage(TextMessage textMessage) {
        BaseWsData baseWsData = transferDataFromBase(textMessage);
        if (baseWsData != null) {
            wsDataPushService.pushDataByWs(baseWsData);
        }
    }

    private BaseWsData transferDataFromBase(TextMessage textMessage) {
        Map<String, Object> wsOriginalData;
        try {
            wsOriginalData = objectMapper.readValue(
                    textMessage.getPayload(), new TypeReference<Map<String, Object>>() { });
        } catch (Exception e) {
            logger.error("序列化ws原始数据失败，原始数据=>[{}]", textMessage.getPayload(), e);
            return null;
        }

        try {
            int action = resolveAction(wsOriginalData.get("action"));
            Jx3WsEventEnum wsEvent = Jx3WsEventEnum.findByActionCode(action);
            logger.info("接收到 JX3API WS 消息，action=>{}，event=>{}，category=>{}",
                    action,
                    wsEvent == null ? "UNKNOWN" : wsEvent.getSummary(),
                    wsEvent == null ? "UNKNOWN" : wsEvent.getCategory());
            Object eventBody = wsOriginalData.get("data");
            if (eventBody == null) {
                eventBody = wsOriginalData.get("detail");
            }
            if (eventBody == null) {
                logger.warn("JX3API WS 消息缺少 data/detail，已忽略，action=>{}", action);
                return null;
            }
            Class<BaseWsData> dataClass = WsActionDataManager.getWsDataByAction(action);
            BaseWsData data = dataClass == null
                    ? objectMapper.convertValue(eventBody, GenericWsData.class)
                    : objectMapper.convertValue(eventBody, dataClass);
            if (data == null) {
                logger.warn("JX3API WS 消息正文解析结果为空，已忽略，action=>{}", action);
                return null;
            }
            data.setAction(action);
            data.setEventFingerprint(fingerprint(wsOriginalData));
            return data;
        } catch (Exception e) {
            logger.error("序列化wsData数据失败，原始数据=>[{}]", textMessage.getPayload(), e);
            return null;
        }
    }

    private int resolveAction(Object actionValue) {
        if (actionValue instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(actionValue));
    }

    private String fingerprint(Map<String, Object> payload) {
        try {
            byte[] canonicalJson = objectMapper.writeValueAsBytes(canonicalize(payload));
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonicalJson);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", e);
        } catch (Exception e) {
            throw new IllegalArgumentException("无法为 JX3API WS 事件生成指纹", e);
        }
    }

    private Object canonicalize(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sorted = new TreeMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                sorted.put(String.valueOf(entry.getKey()), canonicalize(entry.getValue()));
            }
            return sorted;
        }
        if (value instanceof List<?> list) {
            List<Object> canonical = new ArrayList<>(list.size());
            for (Object item : list) {
                canonical.add(canonicalize(item));
            }
            return canonical;
        }
        return value;
    }
}