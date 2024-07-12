package com.person.botjava.contants.channel;

/**
 * @author grafie.chen
 * @since 2024/7/12  16:03
 */
public class SpeakPermission {
    private final Integer code;
    private final String desc;

    SpeakPermission(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
