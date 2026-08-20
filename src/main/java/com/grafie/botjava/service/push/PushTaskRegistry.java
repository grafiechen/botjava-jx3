package com.grafie.botjava.service.push;

import com.grafie.botjava.jx3.ws.data.Jx3WsEventEnum;
import com.grafie.botjava.qq.group.QqGroupLifecycleEventType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 推送任务注册表。QQ/JX3API WS 事件和项目自定义定时任务都在此使用稳定任务编码。
 */
@Component
public class PushTaskRegistry {

    public static final String MONGO_DAILY_PROGRESS = "MONGO_DAILY_PROGRESS";

    private final List<PushTaskDefinition> definitions;
    private final Map<String, PushTaskDefinition> byCode;
    private final Map<Integer, PushTaskDefinition> byJx3WsAction;
    private final Map<QqGroupLifecycleEventType, PushTaskDefinition> byQqWsEvent;

    public PushTaskRegistry() {
        List<PushTaskDefinition> registered = new ArrayList<>();
        Map<QqGroupLifecycleEventType, PushTaskDefinition> qqEvents =
                new EnumMap<>(QqGroupLifecycleEventType.class);
        for (QqGroupLifecycleEventType event : QqGroupLifecycleEventType.values()) {
            PushTaskDefinition definition = new PushTaskDefinition(
                    "QQ_WS_" + event.getPayloadType(),
                    qqSubscriptionName(event),
                    "群状态",
                    PushTaskSource.QQ_WS,
                    null,
                    true);
            registered.add(definition);
            qqEvents.put(event, definition);
        }
        for (Jx3WsEventEnum event : Jx3WsEventEnum.values()) {
            registered.add(new PushTaskDefinition(
                    "JX3_WS_" + event.getActionCode(),
                    jx3SubscriptionName(event),
                    event.getCategory(),
                    PushTaskSource.JX3API_WS,
                    event.getActionCode(),
                    true));
        }
        registered.add(new PushTaskDefinition(
                MONGO_DAILY_PROGRESS,
                "Mongo日常进度",
                "脚本状态",
                PushTaskSource.SCHEDULED,
                null,
                true));
        registered.sort(Comparator.comparing(PushTaskDefinition::source)
                .thenComparing(PushTaskDefinition::category)
                .thenComparing(PushTaskDefinition::displayName));
        this.definitions = List.copyOf(registered);

        Map<String, PushTaskDefinition> codeIndex = new LinkedHashMap<>();
        Map<Integer, PushTaskDefinition> actionIndex = new LinkedHashMap<>();
        for (PushTaskDefinition definition : definitions) {
            if (codeIndex.put(definition.code(), definition) != null) {
                throw new IllegalStateException("重复的推送任务编码：" + definition.code());
            }
            if (definition.wsActionCode() != null
                    && actionIndex.put(definition.wsActionCode(), definition) != null) {
                throw new IllegalStateException("重复的 JX3API WS action：" + definition.wsActionCode());
            }
        }
        this.byCode = Map.copyOf(codeIndex);
        this.byJx3WsAction = Map.copyOf(actionIndex);
        this.byQqWsEvent = Map.copyOf(qqEvents);
    }

    public List<PushTaskDefinition> all() {
        return definitions;
    }

    public Optional<PushTaskDefinition> find(String nameOrCode) {
        if (nameOrCode == null || nameOrCode.isBlank()) {
            return Optional.empty();
        }
        String value = nameOrCode.trim();
        PushTaskDefinition byExactCode = byCode.get(value.toUpperCase(Locale.ROOT));
        if (byExactCode != null) {
            return Optional.of(byExactCode);
        }
        return definitions.stream()
                .filter(definition -> definition.displayName().equals(value))
                .findFirst();
    }

    public Optional<PushTaskDefinition> findByWsAction(int actionCode) {
        return Optional.ofNullable(byJx3WsAction.get(actionCode));
    }

    public Optional<PushTaskDefinition> findByQqWsEvent(QqGroupLifecycleEventType eventType) {
        return Optional.ofNullable(byQqWsEvent.get(eventType));
    }

    private static String jx3SubscriptionName(Jx3WsEventEnum event) {
        return switch (event) {
            case CASTLE_LEADER -> "关隘首领事件";
            case SYSTEM_CASTLE_LEADER -> "关隘首领通知";
            default -> event.getSummary();
        };
    }

    private static String qqSubscriptionName(QqGroupLifecycleEventType event) {
        return switch (event) {
            case ROBOT_ADDED -> "机器人加入群聊";
            case ROBOT_REMOVED -> "机器人退出群聊";
            case PROACTIVE_MESSAGES_ACCEPTED -> "主动消息授权开启";
            case PROACTIVE_MESSAGES_REJECTED -> "主动消息授权关闭";
        };
    }
}