package com.grafie.botjava.entity.dto.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 单聊/群聊回复消息通用结构体
 * @author grafie.chen
 * @since 2025/1/23  15:54
 */
@Data
public class MessageInfo {
    /**
     * 文本内容
     */
    private String content;
    /**
     * 消息类型：0 是文本，2 是 markdown， 3 ark，4 embed，7 media 富媒体
     */
    @JsonProperty(value = "msg_type")
    private String msgType;
    /**
     * Markdown对象
     */
    private MarkdownDto markdown;
    /**
     * Keyboard对象
     */
    private KeyboardDto keyboard;
    /**
     * Ark对象
     */
    private ArkDto ark;
    /**
     * 富媒体单聊的file_info
     */
    private MediaDto media;
    /**
     * 【暂未支持】消息引用
     */
    private MessageReferenceDto messageReference;
    /**
     * 前置收到的事件 ID，用于发送被动消息，支持事件："INTERACTION_CREATE"、"C2C_MSG_RECEIVE"、"FRIEND_ADD"
     */
    @JsonProperty(value = "event_id")
    private String eventId;
    /**
     * 前置收到的用户发送过来的消息 ID，用于发送被动（回复）消息
     */
    @JsonProperty(value = "msg_id")
    private String msgId;
    /**
     * 回复消息的序号，与 msg_id 联合使用，避免相同消息id回复重复发送，不填默认是1。相同的 msg_id + msg_seq 重复发送会失败。
     */
    @JsonProperty(value = "msg_seq")
    private Integer msgSeq;
}
