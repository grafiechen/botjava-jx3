package com.grafie.botjava.entity.dto.c2c;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.grafie.botjava.entity.dto.common.ArkDataDto;
import com.grafie.botjava.entity.dto.common.AuthorDto;
import com.grafie.botjava.entity.dto.common.MessageAttachmentDto;
import com.grafie.botjava.entity.dto.common.MessageSceneDto;
import com.grafie.botjava.entity.dto.common.MsgElementDto;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class C2cMessageCreateDto {
    private String id;
    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private LocalDateTime timestamp;

    private AuthorDto author;

    @JsonProperty("message_scene")
    private MessageSceneDto messageScene;

    @JsonProperty("message_type")
    private Integer messageType;

    private List<MessageAttachmentDto> attachments;

    @JsonProperty("ark_data")
    private ArkDataDto arkData;

    @JsonProperty("msg_elements")
    private List<MsgElementDto> msgElements;

    public String userOpenId() {
        if (author == null) {
            return null;
        }
        if (author.getUserOpenid() != null && !author.getUserOpenid().isBlank()) {
            return author.getUserOpenid();
        }
        return author.getId();
    }
}