package com.person.botjava.contants.channel;

/**
 * 目前只有文字子频道具有 ChannelSubType 二级分类，同时二级分类也可能会不断增加，开发者也需要注意对未知 ID 的处理
 *
 * @author grafie.chen
 * @since 2024/7/12  15:56
 */
public enum ChannelSubType {
    SMALL_CHAT(0,"闲聊"),
    ANNOUNCEMENT(1,"公告"),
    TIPS(2,"攻略"),
    TEAM(3,"开黑")
    ;
    private final Integer code;
    private final String desc;

    ChannelSubType(Integer code, String desc) {
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
