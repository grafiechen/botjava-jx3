package com.grafie.botjava.jx3.ws.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.grafie.botjava.util.TimeUtils;
import lombok.Data;

/**
 * 基础ws变量
 *
 * @author Grafie
 * @since 1.0.0
 */
@Data
public class BaseWsData {
    /*
     * 绝大部分消息里面都有以下值，故放到基础类，部分事件无此相关字段的话，请忽略
     * */
    /**
     * ws事件的actionCode
     */
    private Integer action;
    /**
     * 剑网三 大区
     */
    private String zone;
    /**
     * 剑网三 服务器
     */
    private String server;
    /**
     * 时间
     */
    private String time;

    /**
     * 对 WS 原始帧计算的稳定指纹，仅用于主动推送去重，不参与消息正文序列化。
     */
    @JsonIgnore
    private String eventFingerprint;

    public void setTime(Long time) {
        if (time == null) {
            return;
        }
        this.time = TimeUtils.timeFormatting(time);
    }

}
