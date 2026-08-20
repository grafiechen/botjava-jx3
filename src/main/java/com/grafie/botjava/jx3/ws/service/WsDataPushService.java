package com.grafie.botjava.jx3.ws.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.ws.data.BaseWsData;
import com.grafie.botjava.service.push.GroupPushDispatcher;
import com.grafie.botjava.service.push.PushTaskDefinition;
import com.grafie.botjava.service.push.PushTaskRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * JX3API WebSocket 实时事件主动推送入口。
 */
@Slf4j
@Service
public class WsDataPushService {

    private static final int MAX_FIELDS = 12;
    private static final Set<String> HIDDEN_FIELDS = Set.of(
            "action", "id", "token", "url", "view", "icon", "avatar", "eventFingerprint");
    private static final Map<String, String> FIELD_NAMES = Map.ofEntries(
            Map.entry("zone", "大区"), Map.entry("server", "服务器"),
            Map.entry("time", "时间"), Map.entry("date", "日期"),
            Map.entry("name", "名称"), Map.entry("event", "事件"),
            Map.entry("roleName", "角色"), Map.entry("mapName", "地图"),
            Map.entry("sender", "发送者"), Map.entry("recipient", "接收者"),
            Map.entry("status", "状态"), Map.entry("title", "标题"),
            Map.entry("tags", "分类"), Map.entry("tieba", "贴吧"),
            Map.entry("classType", "分类"), Map.entry("newVersion", "新版本"),
            Map.entry("nowVersion", "当前版本"), Map.entry("packageNum", "文件数量"),
            Map.entry("packageSize", "更新大小"), Map.entry("castle", "关隘"),
            Map.entry("start", "开始时间"), Map.entry("site", "地点"),
            Map.entry("desc", "说明"), Map.entry("horse", "马驹"),
            Map.entry("campName", "阵营"), Map.entry("tongName", "帮会"),
            Map.entry("amount", "金额"), Map.entry("level", "等级"));

    private final PushTaskRegistry taskRegistry;
    private final GroupPushDispatcher pushDispatcher;
    private final ObjectMapper objectMapper;

    public WsDataPushService(PushTaskRegistry taskRegistry,
                             GroupPushDispatcher pushDispatcher,
                             ObjectMapper objectMapper) {
        this.taskRegistry = taskRegistry;
        this.pushDispatcher = pushDispatcher;
        this.objectMapper = objectMapper;
    }

    /**
     * JX3API WS 事件只做实时转发，不再包含星期等业务过滤。
     */
    public void pushDataByWs(BaseWsData data) {
        if (data == null || data.getAction() == null) {
            log.warn("JX3API WS 推送缺少 action，已忽略");
            return;
        }
        PushTaskDefinition task = taskRegistry.findByWsAction(data.getAction()).orElse(null);
        if (task == null) {
            log.warn("JX3API WS 事件尚未注册推送任务，action=>{}", data.getAction());
            return;
        }
        if (data.getEventFingerprint() == null || data.getEventFingerprint().isBlank()) {
            log.error("JX3API WS 事件缺少去重指纹，拒绝主动推送，action=>{}", data.getAction());
            return;
        }
        pushDispatcher.publishWsEvent(task, data.getEventFingerprint(),
                BotResponse.text(buildText(task, data)));
    }

    private String buildText(PushTaskDefinition task, BaseWsData data) {
        Map<String, Object> values = objectMapper.convertValue(
                data, new TypeReference<LinkedHashMap<String, Object>>() { });
        StringBuilder text = new StringBuilder("【JX3API推送】").append(task.displayName());
        int appended = 0;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (appended >= MAX_FIELDS || HIDDEN_FIELDS.contains(entry.getKey())
                    || entry.getValue() == null || String.valueOf(entry.getValue()).isBlank()) {
                continue;
            }
            String value = displayValue(task, entry.getKey(), entry.getValue());
            if (value.length() > 160) {
                value = value.substring(0, 160) + "...";
            }
            text.append("\n")
                    .append(FIELD_NAMES.getOrDefault(entry.getKey(), entry.getKey()))
                    .append("：")
                    .append(value);
            appended++;
        }
        return text.toString();
    }

    private String displayValue(PushTaskDefinition task, String field, Object value) {
        if (task.wsActionCode() != null && task.wsActionCode() == 2001 && "status".equals(field)) {
            return "1".equals(String.valueOf(value)) ? "已开服" : "维护中";
        }
        return String.valueOf(value);
    }
}