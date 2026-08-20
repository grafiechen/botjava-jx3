package com.grafie.botjava.qq.group;

import com.grafie.botjava.entity.dto.group.event.QqGroupLifecycleEventDto;
import com.grafie.botjava.service.GroupConfigurationService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 将 QQ 平台群状态同步到本地整群配置。
 */
@Component
@Order(0)
public class GroupConfigurationLifecycleHandler implements QqGroupLifecycleHandler {

    private final GroupConfigurationService groupConfigurationService;

    public GroupConfigurationLifecycleHandler(GroupConfigurationService groupConfigurationService) {
        this.groupConfigurationService = groupConfigurationService;
    }

    @Override
    public boolean supports(QqGroupLifecycleEventType eventType) {
        return true;
    }

    @Override
    public void handle(QqGroupLifecycleEventType eventType, QqGroupLifecycleEventDto event) {
        boolean platformAllowed = eventType == QqGroupLifecycleEventType.PROACTIVE_MESSAGES_ACCEPTED;
        groupConfigurationService.setActiveMessagesPlatformAllowed(event.getGroupOpenId(), platformAllowed);
    }
}
