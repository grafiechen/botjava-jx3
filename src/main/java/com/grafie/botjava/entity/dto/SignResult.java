package com.grafie.botjava.entity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author grafie.chen
 * @since 2025/1/22  12:01
 */
@Data
public class SignResult {
    /**
     * 签名
     */
    private String signature;
    /**
     * 需要计算签名的字符串
     */
    @JsonProperty(value = "plain_token")
    private String plainToken;
}
