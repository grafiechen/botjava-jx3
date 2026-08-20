package com.grafie.botjava.qq.group;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.event.QqGroupLifecycleEventDto;
import com.grafie.botjava.service.push.GroupPushDispatcher;
import com.grafie.botjava.service.push.PushTaskDefinition;
import com.grafie.botjava.service.push.PushTaskRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class QqGroupLifecyclePushHandlerTest {

    @Test
    void shouldPublishQqWsEventToItsOwnGroupWithStableFingerprint() {
        PushTaskRegistry registry = new PushTaskRegistry();
        GroupPushDispatcher dispatcher = mock(GroupPushDispatcher.class);
        QqGroupLifecyclePushHandler handler =
                new QqGroupLifecyclePushHandler(registry, dispatcher);
        QqGroupLifecycleEventDto event = new QqGroupLifecycleEventDto();
        event.setGroupOpenId("group-a");
        event.setOperatorMemberOpenId("member-a");
        event.setTimestamp("2026-08-14T18:30:00+09:00");

        handler.handle(QqGroupLifecycleEventType.PROACTIVE_MESSAGES_ACCEPTED, event);

        ArgumentCaptor<PushTaskDefinition> taskCaptor =
                ArgumentCaptor.forClass(PushTaskDefinition.class);
        ArgumentCaptor<String> fingerprintCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<BotResponse> responseCaptor = ArgumentCaptor.forClass(BotResponse.class);
        verify(dispatcher).publishWsEventToGroup(
                taskCaptor.capture(),
                org.mockito.ArgumentMatchers.eq("group-a"),
                fingerprintCaptor.capture(),
                responseCaptor.capture());
        assertThat(taskCaptor.getValue().displayName()).isEqualTo("主动消息授权开启");
        assertThat(fingerprintCaptor.getValue()).matches("[0-9a-f]{64}");
        assertThat(responseCaptor.getValue().getContent())
                .contains("QQ WebSocket推送", "主动消息授权开启", "2026-08-14T18:30:00+09:00");
    }
}