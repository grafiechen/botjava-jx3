package com.grafie.botjava.qq.group;

import com.grafie.botjava.entity.dto.group.event.QqGroupLifecycleEventDto;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 将一个 QQ 群状态事件分发给全部显式订阅的业务处理器。
 */
@Component
public class QqGroupLifecycleHandlerRegistry {

    private final List<QqGroupLifecycleHandler> handlers;

    public QqGroupLifecycleHandlerRegistry(List<QqGroupLifecycleHandler> handlers) {
        this.handlers = List.copyOf(handlers);
    }

    public int dispatch(QqGroupLifecycleEventType eventType, QqGroupLifecycleEventDto event) {
        int handled = 0;
        for (QqGroupLifecycleHandler handler : handlers) {
            if (handler.supports(eventType)) {
                handler.handle(eventType, event);
                handled++;
            }
        }
        return handled;
    }
}
