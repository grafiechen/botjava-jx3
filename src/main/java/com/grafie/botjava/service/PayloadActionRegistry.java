package com.grafie.botjava.service;

import com.grafie.botjava.action.BaseAction;
import com.grafie.botjava.contants.PayloadTEnum;
import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * QQ dispatch 事件处理器注册表。
 */
@Component
public class PayloadActionRegistry {

    private final Map<PayloadTEnum, BaseAction> actions;

    public PayloadActionRegistry(List<BaseAction> actionBeans) {
        Map<Class<?>, BaseAction> beansByType = new LinkedHashMap<>();
        for (BaseAction action : actionBeans) {
            Class<?> targetClass = AopUtils.getTargetClass(action);
            BaseAction previous = beansByType.put(targetClass, action);
            if (previous != null) {
                throw new IllegalStateException("Payload Action 重复注册：" + targetClass.getName());
            }
        }

        Map<PayloadTEnum, BaseAction> registered = new EnumMap<>(PayloadTEnum.class);
        for (PayloadTEnum eventType : PayloadTEnum.values()) {
            BaseAction action = beansByType.get(eventType.getClasz());
            if (action == null) {
                throw new IllegalStateException(
                        "Payload 事件缺少处理器：" + eventType.getValue() + " -> " + eventType.getClasz().getName()
                );
            }
            registered.put(eventType, action);
        }
        this.actions = Map.copyOf(registered);
    }

    public BaseAction get(PayloadTEnum eventType) {
        BaseAction action = actions.get(eventType);
        if (action == null) {
            throw new IllegalArgumentException("未注册的 Payload 事件：" + eventType);
        }
        return action;
    }
}
