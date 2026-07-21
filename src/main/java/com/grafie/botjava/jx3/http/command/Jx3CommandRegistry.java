package com.grafie.botjava.jx3.http.command;

import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.util.REGEX;
import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JX3 群指令注册表。
 * <p>
 * 统一负责输入规范化、正则匹配和 Spring Action 定位，并在启动时校验所有已注册指令都有处理器。
 */
@Component
public class Jx3CommandRegistry {

    private final Map<REGEX, Jx3BaseAction> actions;

    public Jx3CommandRegistry(List<Jx3BaseAction> actionBeans) {
        Map<Class<? extends Jx3BaseAction>, Jx3BaseAction> beansByType = new LinkedHashMap<>();
        for (Jx3BaseAction action : actionBeans) {
            Class<?> targetClass = AopUtils.getTargetClass(action);
            if (!Jx3BaseAction.class.isAssignableFrom(targetClass)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Class<? extends Jx3BaseAction> actionClass = (Class<? extends Jx3BaseAction>) targetClass;
            Jx3BaseAction previous = beansByType.put(actionClass, action);
            if (previous != null) {
                throw new IllegalStateException("JX3 Action 重复注册：" + actionClass.getName());
            }
        }

        Map<REGEX, Jx3BaseAction> registered = new EnumMap<>(REGEX.class);
        for (REGEX definition : REGEX.values()) {
            Jx3BaseAction action = beansByType.get(definition.getBaseAction());
            if (action == null) {
                throw new IllegalStateException(
                        "JX3 指令缺少处理器：" + definition.name() + " -> " + definition.getBaseAction().getName()
                );
            }
            registered.put(definition, action);
        }
        this.actions = Map.copyOf(registered);
    }

    public Optional<ResolvedJx3Command> resolve(String input) {
        String command = REGEX.normalizeCommand(input);
        REGEX definition = REGEX.matchEnum(command);
        if (definition == null) {
            return Optional.empty();
        }
        return Optional.of(new ResolvedJx3Command(
                command,
                definition,
                CommandArguments.of(definition.handleEncounter(command)),
                actions.get(definition)
        ));
    }
}
