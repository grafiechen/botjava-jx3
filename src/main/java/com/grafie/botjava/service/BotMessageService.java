package com.grafie.botjava.service;

import com.grafie.botjava.action.BaseAction;
import com.grafie.botjava.action.CallbackValidAction;
import com.grafie.botjava.contants.OpCode;
import com.grafie.botjava.contants.PayloadTEnum;
import com.grafie.botjava.entity.dto.payload.Payload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author grafie.chen
 * @since 2025/1/22  14:55
 */
@Slf4j
@Service
public class BotMessageService {

    private final PayloadActionRegistry actionRegistry;

    public BotMessageService(PayloadActionRegistry actionRegistry) {
        this.actionRegistry = actionRegistry;
    }

    /**
     * 消息处理
     *
     * @param payload 外部数据
     * @return 具体返回值，根据官方文档要求来
     */
    public Object dealMessage(Payload payload) {
        log.info("处理 QQ payload，op=>{}，t=>{}，sequence=>{}",
                payload.getOp(), payload.getT(), payload.getS());
        if (payload.getOp().equals(OpCode.CALLBACK_VALID.getCode())) {
            return CallbackValidAction.doSign(payload);
        } else if (payload.getOp().equals(OpCode.DISPATCH.getCode())) {
            return dealDispatchMessage(payload);
        } else {
            log.error("暂不支持 op 为 {} 的消息处理，t=>{}", payload.getOp(), payload.getT());
        }
        return null;
    }

    private Object dealDispatchMessage(Payload payload) {
        log.info("处理 QQ dispatch，t=>{}，sequence=>{}", payload.getT(), payload.getS());
        PayloadTEnum payloadTEnum = PayloadTEnum.getByValue(payload.getT());
        if (payloadTEnum == null) {
            log.error("未能根据 t 值 {} 找到合适的事件处理器", payload.getT());
            return null;
        }
        BaseAction baseAction = actionRegistry.get(payloadTEnum);
        return baseAction.doAction(payload);
    }

}

