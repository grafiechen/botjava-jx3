package com.grafie.botjava.entity.dto.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author grafie.chen
 * @since 2025/1/22  12:37
 */
@Data
public class MessageSceneDto {
    private String source;
    @JsonProperty(value = "callback_data")
    private String callbackData;
}
