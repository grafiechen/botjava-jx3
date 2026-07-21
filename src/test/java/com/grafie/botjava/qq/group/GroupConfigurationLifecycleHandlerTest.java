package com.grafie.botjava.qq.group;

import com.grafie.botjava.entity.dto.group.event.QqGroupLifecycleEventDto;
import com.grafie.botjava.service.GroupConfigurationService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GroupConfigurationLifecycleHandlerTest {

    @Test
    void shouldSynchronizePlatformProactiveMessageAuthorization() {
        GroupConfigurationService configuration = mock(GroupConfigurationService.class);
        GroupConfigurationLifecycleHandler handler = new GroupConfigurationLifecycleHandler(configuration);
        QqGroupLifecycleEventDto event = new QqGroupLifecycleEventDto();
        event.setGroupOpenId("group-1");

        handler.handle(QqGroupLifecycleEventType.ROBOT_ADDED, event);
        handler.handle(QqGroupLifecycleEventType.ROBOT_REMOVED, event);
        handler.handle(QqGroupLifecycleEventType.PROACTIVE_MESSAGES_REJECTED, event);
        handler.handle(QqGroupLifecycleEventType.PROACTIVE_MESSAGES_ACCEPTED, event);

        verify(configuration, org.mockito.Mockito.times(3))
                .setActiveMessagesPlatformAllowed("group-1", false);
        verify(configuration).setActiveMessagesPlatformAllowed("group-1", true);
    }
}
