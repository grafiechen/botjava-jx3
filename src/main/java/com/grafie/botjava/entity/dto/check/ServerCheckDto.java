package com.grafie.botjava.entity.dto.check;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author grafie.chen
 * @since 2025/1/22  12:05
 */
@Data
public class ServerCheckDto {
    /**
     * 需要计算签名的字符串
     */
    @JsonProperty(value = "plain_token")
    private String plainToken;
    /**
     * 计算签名使用时间戳
     */
    @JsonProperty(value = "event_ts")
    private String eventTs;
}
