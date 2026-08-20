package com.grafie.botjava.jx3.http;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

/**
 * 最外层返回值
 *
 * @author Grafie
 * @since 1.0.0
 */
@Data
public class RequestResult {
    /**
     * 返回状态值
     */
    private Integer code;
    /**
     * 提示内容
     */
    private String msg;
    /**
     * 实际返回值
     */
    private Object data;
    /**
     * 时间戳
     */
    private Long time;
    /**
     * 本次 HTTP 调用的原始响应体，仅用于日志排障，不进入 JSON 序列化或缓存。
     */
    @JsonIgnore
    private String rawResponseBody;
}
