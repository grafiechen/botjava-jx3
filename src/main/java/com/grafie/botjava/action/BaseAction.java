package com.grafie.botjava.action;

import com.grafie.botjava.entity.dto.payload.Payload;
import lombok.extern.slf4j.Slf4j;
import com.grafie.botjava.util.SensitiveDataUtil;

/**
 * @author grafie.chen
 * @since 2025/1/22  12:49
 */
@Slf4j
public abstract class BaseAction {

    public Object doAction(Payload payload) {
        log.info("开始处理消息事件，op=>{}，t=>{}，sequence=>{}",
                payload.getOp(), payload.getT(), payload.getS());
        try {
            return deal(payload);
        } catch (Exception e) {
            log.error("处理消息事件失败，op=>{}，t=>{}，reason=>{}",
                    payload.getOp(), payload.getT(), SensitiveDataUtil.summarize(e));
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
