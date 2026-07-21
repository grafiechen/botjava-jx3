package com.grafie.botjava.qq.group;

import com.grafie.botjava.entity.dto.group.event.QqGroupLifecycleEventDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QqGroupLifecycleHandlerRegistryTest {

    @Test
    void shouldNotifyEverySubscribedObserver() {
        QqGroupLifecycleHandler first = mock(QqGroupLifecycleHandler.class);
        QqGroupLifecycleHandler second = mock(QqGroupLifecycleHandler.class);
        QqGroupLifecycleEventDto event = new QqGroupLifecycleEventDto();
        when(first.supports(QqGroupLifecycleEventType.ROBOT_ADDED)).thenReturn(true);
        when(second.supports(QqGroupLifecycleEventType.ROBOT_ADDED)).thenReturn(true);
        QqGroupLifecycleHandlerRegistry registry =
                new QqGroupLifecycleHandlerRegistry(List.of(first, second));

        int handled = registry.dispatch(QqGroupLifecycleEventType.ROBOT_ADDED, event);

        assertEquals(2, handled);
        verify(first).handle(QqGroupLifecycleEventType.ROBOT_ADDED, event);
        verify(second).handle(QqGroupLifecycleEventType.ROBOT_ADDED, event);
    }
}
