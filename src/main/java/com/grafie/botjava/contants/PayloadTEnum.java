package com.grafie.botjava.contants;

import com.grafie.botjava.action.GroupAtMessageAction;
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
    GROUP_MESSAGE_CREATE("GROUP_MESSAGE_CREATE", GroupAtMessageAction.class);


    @Getter
    private final String value;
    @Getter
    private final Class clasz;

    PayloadTEnum(String value, Class clasz) {
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
