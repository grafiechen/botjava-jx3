package com.person.botjava.ws.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 重连参数
 *
 * @author grafie.chen
 * @since 2024/7/16  16:59
 */
@Data
public class ResumeDto {

    /**
     * 配置的token
     */
    private String token;
    /**
     * 文档没写是哪个，只能选择使用每次建立连接时候返回的session_id
     */
    @JsonProperty("session_id")
    private String sessionId;
    /**
     * 最近一次接收到的消息序列号。 用于补发seq之后的事件
     */
    private Integer seq;

}
