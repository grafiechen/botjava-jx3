package com.grafie.botjava.service;

import com.grafie.botjava.action.BaseAction;
import com.grafie.botjava.action.CallbackValidAction;
import com.grafie.botjava.contants.OpCode;
import com.grafie.botjava.contants.PayloadTEnum;
import com.grafie.botjava.entity.dto.payload.Payload;
import com.grafie.botjava.util.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author grafie.chen
 * @since 2025/1/22  14:55
 */
@Slf4j
@Service
public class BotMessageService {
    /**
     * 消息处理
     *
     * @param payload 外部数据
     * @return 具体返回值，根据官方文档要求来
     */
    public Object dealMessage(Payload payload) {
        if (payload.getOp().equals(OpCode.CALLBACK_VALID.getCode())) {
            return CallbackValidAction.doSign(payload);
        } else if (payload.getOp().equals(OpCode.DISPATCH.getCode())) {
            return dealDispatchMessage(payload);
        } else {
            log.error("暂不支持op为{}的消息处理。消息内容=>{}", payload.getOp(), payload);
        }
        return null;
    }

    private Object dealDispatchMessage(Payload payload) {
        PayloadTEnum payloadTEnum = PayloadTEnum.getByValue(payload.getT());
        if (payloadTEnum == null) {
            log.error("未能根据t值{}找到合适的枚举，payload=>{}", payload.getT(), payload);
            return null;
        }
        BaseAction baseAction = (BaseAction) SpringContextUtil.getBean(payloadTEnum.getClasz());
        return baseAction.doAction(payload);
    }

}

