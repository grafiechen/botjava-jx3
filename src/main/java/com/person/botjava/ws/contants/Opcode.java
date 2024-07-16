package com.person.botjava.ws.contants;

import java.util.HashMap;
import java.util.Map;

/**
 * 客户端操作描述枚举
 *
 * @author grafie.chen
 * @since 2024/7/12  16:32
 */
public enum Opcode {
    /**
     * CODE	名称	                客户端操作	    描述
     * 0	Dispatch	        Receive	        服务端进行消息推送
     * 1	Heartbeat	        Send/Receive	客户端或服务端发送心跳
     * 2	Identify	        Send	        客户端发送鉴权
     * 6	Resume	            Send	        客户端恢复连接
     * 7	Reconnect	        Receive	        服务端通知客户端重新连接
     * 9	Invalid Session 	Receive	        当identify或resume的时候，如果参数有错，服务端会返回该消息
     * 10	Hello	            Receive	        当客户端与网关建立ws连接之后，网关下发的第一条消息
     * 11	Heartbeat ACK	    Receive/Reply	当发送心跳成功之后，就会收到该消息
     * 12	HTTP Callback ACK	Reply	        仅用于 http 回调模式的回包，代表机器人收到了平台推送的数据
     * <p>
     * 客户端操作含义如下：
     * <p>
     * Receive 客户端接收到服务端 push 的消息
     * Send 客户端发送消息
     * Reply 客户端接收到服务端发送的消息之后的回包（HTTP 回调模式）
     */
    DISPATCH(0, "服务端进行消息推送"),
    HEARTBEAT(1, "客户端或服务端发送心跳"),
    IDENTIFY(2, "客户端发送鉴权"),
    RESUME(6, "客户端恢复连接"),
    RECONNECT(7, "服务端通知客户端重新连接"),
    INVALID_SESSION(9, "当identify或resume的时候，如果参数有错，服务端会返回该消息"),
    HELLO(10, "当客户端与网关建立ws连接之后，网关下发的第一条消息"),
    HEARTBEAT_ACK(11, "当发送心跳成功之后，就会收到该消息"),
    HTTP_CALLBACK_ACK(12, "仅用于 http 回调模式的回包，代表机器人收到了平台推送的数据");

    private final Integer code;
    private final String note;
    private static final Map<Integer, Opcode> opcodeMap = new HashMap<>();

    static {
        for (Opcode opcode : values()) {
            opcodeMap.put(opcode.code, opcode);
        }
    }

    Opcode(Integer code, String note) {
        this.code = code;
        this.note = note;
    }

    public Integer getCode() {
        return code;
    }

    public String getNote() {
        return note;
    }

    public static Opcode getByCode(int code) {
        return opcodeMap.get(code);
    }
}
