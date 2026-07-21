package com.grafie.botjava.qq.group;

import com.grafie.botjava.entity.dto.group.event.QqGroupLifecycleEventDto;

/**
 * 群状态事件观察者。不同业务可以独立订阅同一官方事件。
 */
public interface QqGroupLifecycleHandler {

    boolean supports(QqGroupLifecycleEventType eventType);

    void handle(QqGroupLifecycleEventType eventType, QqGroupLifecycleEventDto event);
}
