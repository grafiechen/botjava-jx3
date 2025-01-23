package com.grafie.botjava.entity.dto.group.at;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.grafie.botjava.entity.dto.common.MessageSceneDto;
import com.grafie.botjava.entity.dto.common.AuthorDto;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author grafie.chen
 * @since 2025/1/22  12:35
 */
@Data
public class GroupAtMessageCreateDto {
    private String id;
    /**
     * 内容
     */
    private String content;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private LocalDateTime timestamp;
    private AuthorDto author;
    @JsonProperty(value = "group_id")
    private String groupId;
    @JsonProperty(value = "group_openid")
    private String groupOpenid;
    @JsonProperty(value = "message_scene")
    private MessageSceneDto messageSceneDto;
}
