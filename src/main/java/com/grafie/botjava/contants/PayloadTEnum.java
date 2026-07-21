package com.grafie.botjava.contants;

import com.grafie.botjava.action.BaseAction;
import com.grafie.botjava.action.GroupAtMessageAction;
import com.grafie.botjava.action.GroupLifecycleAction;
import com.grafie.botjava.action.InteractionCreateAction;
import lombok.Getter;

/**
 * payload 结构体中，t 字段枚举
 *
 * @author grafie.chen
 * @since 2024/7/16  17:17
 */
public enum PayloadTEnum {
    /**
     * 群聊被at
     */
    GROUP_AT_MESSAGE_CREATE("GROUP_AT_MESSAGE_CREATE", GroupAtMessageAction.class),
    GROUP_MESSAGE_CREATE("GROUP_MESSAGE_CREATE", GroupAtMessageAction.class),
    GROUP_ADD_ROBOT("GROUP_ADD_ROBOT", GroupLifecycleAction.class),
    GROUP_DEL_ROBOT("GROUP_DEL_ROBOT", GroupLifecycleAction.class),
    GROUP_MSG_RECEIVE("GROUP_MSG_RECEIVE", GroupLifecycleAction.class),
    GROUP_MSG_REJECT("GROUP_MSG_REJECT", GroupLifecycleAction.class),
    INTERACTION_CREATE("INTERACTION_CREATE", InteractionCreateAction.class);


    @Getter
    private final String value;
    @Getter
    private final Class<? extends BaseAction> clasz;

    PayloadTEnum(String value, Class<? extends BaseAction> clasz) {
        this.value = value;
        this.clasz = clasz;
    }

    public static PayloadTEnum getByValue(String value) {
        for (PayloadTEnum payloadTEnum : values()) {
            if (payloadTEnum.getValue().equals(value)) {
                return payloadTEnum;
            }
        }
        return null;
    }
}
