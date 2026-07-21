package com.grafie.botjava.qq.interaction;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.interaction.InteractionCreateDto;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 按互动内容选择唯一业务处理器。
 */
@Component
public class QqInteractionHandlerRegistry {

    private final List<QqInteractionHandler> handlers;

    public QqInteractionHandlerRegistry(List<QqInteractionHandler> handlers) {
        this.handlers = List.copyOf(handlers);
    }

    public BotResponse dispatch(InteractionCreateDto interaction) {
        QqInteractionHandler selected = null;
        for (QqInteractionHandler handler : handlers) {
            if (!handler.supports(interaction)) {
                continue;
            }
            if (selected != null) {
                throw new IllegalStateException("QQ 互动事件匹配到多个处理器："
                        + selected.getClass().getName() + "、" + handler.getClass().getName());
            }
            selected = handler;
        }
        return selected == null ? null : selected.handle(interaction);
    }
}
