package com.grafie.botjava.entity.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 单聊/群聊回复消息通用结构体
 * <p>
 * 不转换下划线为驼峰，便于返回数据处理
 *
 * @author grafie.chen
 * @since 2025/1/23  15:54
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TxMessageInfo {
    /**
     * 文本内容
     */
    private String content;
    /**
     * 消息类型：0 是文本，2 是 markdown， 3 ark，4 embed，7 media 富媒体
     */
    private Integer msg_type;
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
     * Embed对象
     */
    private Object embed;
    /**
     * 富媒体群聊的file_info
     */
    private MediaDto media;
    /**
     * 消息引用
     */
    @JsonProperty(value = "message_reference")
    private MessageReferenceDto messageReference;
    /**
     * 前置收到的事件 ID，用于发送被动消息，支持事件："INTERACTION_CREATE"、"GROUP_ADD_ROBOT"、"GROUP_MSG_RECEIVE"
     */
    private String event_id;
    /**
     * 前置收到的用户发送过来的消息 ID，用于发送被动（回复）消息
     */
    private String msg_id;
    /**
     * 回复消息的序号，与 msg_id 联合使用，避免相同消息id回复重复发送，不填默认是1。相同的 msg_id + msg_seq 重复发送会失败。
     */
    private Integer msg_seq;
}
