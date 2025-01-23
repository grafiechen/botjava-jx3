package com.grafie.botjava.jx3.http;

import lombok.Data;

/**
 * 最外层返回值
 *
 * @author Grafie
 * @since 1.0.0
 */
@Data
public class MessageInfo {
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
}
