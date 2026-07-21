package com.grafie.botjava.qq.group;

import java.util.Arrays;
import java.util.Optional;

/**
 * QQ 官方当前支持的群状态 dispatch 事件。
 */
public enum QqGroupLifecycleEventType {
    ROBOT_ADDED("GROUP_ADD_ROBOT"),
    ROBOT_REMOVED("GROUP_DEL_ROBOT"),
    PROACTIVE_MESSAGES_ACCEPTED("GROUP_MSG_RECEIVE"),
    PROACTIVE_MESSAGES_REJECTED("GROUP_MSG_REJECT");

    private final String payloadType;

    QqGroupLifecycleEventType(String payloadType) {
        this.payloadType = payloadType;
    }

    public String getPayloadType() {
        return payloadType;
    }

    public static Optional<QqGroupLifecycleEventType> fromPayloadType(String payloadType) {
        return Arrays.stream(values())
                .filter(type -> type.payloadType.equals(payloadType))
                .findFirst();
    }
}
