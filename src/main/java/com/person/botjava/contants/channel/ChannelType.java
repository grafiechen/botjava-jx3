package com.person.botjava.contants.channel;

/**
 * 子频道类型枚举
 * 由于QQ频道还在持续的迭代中，经常会有新的子频道类型增加，文档更新不一定及时，开发者识别 ChannelType 时，请注意相关的未知 ID 的处理
 * @author grafie.chen
 * @since 2024/7/12  15:50
 */
public enum ChannelType {
    TEXT(0,"文字子频道"),
    RESERVED(1,"保留，不可用"),
    VOICE(2,"语音子频道"),
    RESERVED_2(3,"保留，不可用"),
    GROUP(4,"子频道分组"),
    LIVE(10005,"直播子频道"),
    APPLICATION(10006,"应用子频道"),
    FORUM(10007,"论坛子频道")
    ;
    private final Integer code;
    private final String desc;

    ChannelType(Integer code, String desc) {
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
