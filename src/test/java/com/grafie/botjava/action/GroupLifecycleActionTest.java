package com.grafie.botjava.action;

import com.grafie.botjava.entity.dto.group.event.QqGroupLifecycleEventDto;
import com.grafie.botjava.entity.dto.payload.Payload;
import com.grafie.botjava.qq.group.QqGroupLifecycleEventType;
import com.grafie.botjava.qq.group.QqGroupLifecycleHandlerRegistry;
import com.grafie.botjava.util.ObjectMapperUtil;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class GroupLifecycleActionTest {

    @Test
    void shouldReplayOfficialPayloadFixtures() throws Exception {
        QqGroupLifecycleHandlerRegistry registry = mock(QqGroupLifecycleHandlerRegistry.class);
        GroupLifecycleAction action = new GroupLifecycleAction(registry);
        List<Fixture> fixtures = List.of(
                new Fixture("group-add-robot.json", QqGroupLifecycleEventType.ROBOT_ADDED),
                new Fixture("group-del-robot.json", QqGroupLifecycleEventType.ROBOT_REMOVED),
                new Fixture("group-msg-receive.json", QqGroupLifecycleEventType.PROACTIVE_MESSAGES_ACCEPTED),
                new Fixture("group-msg-reject.json", QqGroupLifecycleEventType.PROACTIVE_MESSAGES_REJECTED)
        );

        for (Fixture fixture : fixtures) {
            String json = Files.readString(Path.of("src", "test", "resources", "payload", fixture.fileName()));
            Payload payload = ObjectMapperUtil.readValue(json, Payload.class);
            action.doAction(payload);
            verify(registry).dispatch(eq(fixture.eventType()),
                    org.mockito.ArgumentMatchers.argThat(event ->
                            "group-1".equals(event.getGroupOpenId())
                                    && "account-1".equals(event.getOperatorMemberOpenId())));
        }
    }

    @Test
    void shouldParseAndDispatchEverySupportedGroupLifecycleEvent() {
        QqGroupLifecycleHandlerRegistry registry = mock(QqGroupLifecycleHandlerRegistry.class);
        GroupLifecycleAction action = new GroupLifecycleAction(registry);

        for (QqGroupLifecycleEventType eventType : QqGroupLifecycleEventType.values()) {
            action.doAction(payload(eventType.getPayloadType(), "group-" + eventType.ordinal()));
        }

        ArgumentCaptor<QqGroupLifecycleEventDto> eventCaptor =
                ArgumentCaptor.forClass(QqGroupLifecycleEventDto.class);
        for (QqGroupLifecycleEventType eventType : QqGroupLifecycleEventType.values()) {
            verify(registry).dispatch(eq(eventType), eventCaptor.capture());
            QqGroupLifecycleEventDto event = eventCaptor.getValue();
            assertEquals("group-" + eventType.ordinal(), event.getGroupOpenId());
            assertEquals("account-1", event.getOperatorMemberOpenId());
            assertEquals("2026-07-13T18:00:00+09:00", event.getTimestamp());
        }
    }

    @Test
    void shouldRejectEventWithoutGroupOpenId() {
        QqGroupLifecycleHandlerRegistry registry = mock(QqGroupLifecycleHandlerRegistry.class);
        GroupLifecycleAction action = new GroupLifecycleAction(registry);
        Payload payload = payload("GROUP_ADD_ROBOT", " ");

        action.doAction(payload);

        verify(registry, never()).dispatch(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private static Payload payload(String eventType, String groupOpenId) {
        Payload payload = new Payload();
        payload.setOp(0);
        payload.setT(eventType);
        payload.setD(Map.of(
                "timestamp", "2026-07-13T18:00:00+09:00",
                "group_openid", groupOpenId,
                "op_member_openid", "account-1",
                "future_field", "ignored"
        ));
        return payload;
    }

    private record Fixture(String fileName, QqGroupLifecycleEventType eventType) {
    }
}
