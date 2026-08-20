package com.grafie.botjava.entity.dto.qq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * QQ OpenAPI v2 WebSocket 网关地址响应。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QqGatewayDto {

    private String url;
    private Integer shards;

    @JsonProperty("session_start_limit")
    private SessionStartLimit sessionStartLimit;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SessionStartLimit {
        private Integer total;
        private Integer remaining;

        @JsonProperty("reset_after")
        private Integer resetAfter;

        @JsonProperty("max_concurrency")
        private Integer maxConcurrency;
    }
}
