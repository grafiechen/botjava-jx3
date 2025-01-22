package com.grafie.botjava.entity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    private LocalDateTime timestamp;
    private AuthorDto author;
    @JsonProperty(value = "group_id")
    private String groupId;
    @JsonProperty(value = "group_openid")
    private String groupOpenid;
}
