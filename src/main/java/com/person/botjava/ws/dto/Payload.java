package com.person.botjava.ws.dto;

import lombok.Data;

/**
 * 最外层的payload数据结构
 * @author grafie.chen
 * @since 2024/7/12  16:29
 */
@Data
public class Payload {
    /**
     * opcode
     */
    private Integer op;
    /**
     * 下行消息都会有一个序列号，标识消息的唯一性，客户端需要再发送心跳的时候，携带客户端收到的最新的s。
     */
    private Integer s;
    /**
     * t和d 主要是用在op为 0 Dispatch 的时候，t 代表事件类型，d 代表事件内容，不同事件类型的事件内容格式都不同，请注意识别
     */
    private Object d;
    /**
     * t和d 主要是用在op为 0 Dispatch 的时候，t 代表事件类型，d 代表事件内容，不同事件类型的事件内容格式都不同，请注意识别
     */
    private String t;
}
