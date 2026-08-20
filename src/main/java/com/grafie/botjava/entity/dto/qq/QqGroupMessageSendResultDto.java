package com.grafie.botjava.entity.dto.qq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * QQ 群聊消息发送结果。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QqGroupMessageSendResultDto {

    private String id;
    private String timestamp;

    @JsonProperty("ext_info")
    private ExtInfo extInfo;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExtInfo {
        @JsonProperty("ref_idx")
        private String refIdx;
    }
}
