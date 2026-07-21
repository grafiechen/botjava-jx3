package com.grafie.botjava.entity.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * QQ 消息引用。
 *
 * @author grafie.chen
 * @since 2025/1/23  15:57
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageReferenceDto {

    @JsonProperty("message_id")
    private String messageId;

    @JsonProperty("ignore_get_message_error")
    private Boolean ignoreGetMessageError;
}
