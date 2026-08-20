package com.grafie.botjava.entity.dto.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * QQ 接收侧消息元素，支持引用消息等嵌套结构。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MsgElementDto {

    @JsonProperty("msg_idx")
    private String msgIdx;

    private AuthorDto author;

    @JsonProperty("message_type")
    private Integer messageType;

    private String content;
    private List<MessageAttachmentDto> attachments;

    @JsonProperty("ark_data")
    private ArkDataDto arkData;

    @JsonProperty("msg_elements")
    private List<MsgElementDto> msgElements;
}
