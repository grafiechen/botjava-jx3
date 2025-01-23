package com.grafie.botjava.action;

import com.grafie.botjava.entity.dto.payload.Payload;
import lombok.extern.slf4j.Slf4j;

/**
 * @author grafie.chen
 * @since 2025/1/22  12:49
 */
@Slf4j
public abstract class BaseAction {

    public Object doAction(Payload payload) {
        log.info("开始处理指令，请求参数=>{}", payload);
        try {
            return deal(payload);
        } catch (Exception e) {
            log.error("处理命令出现异常，请求参数=>{}", payload, e);
        }
        return null;
    }

    /**
     * 基础模板
     *
     * @param payload 请求参数
     */
    protected abstract Object deal(Payload payload) throws Exception;
}
