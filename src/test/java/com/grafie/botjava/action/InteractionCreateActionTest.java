package com.grafie.botjava.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.payload.Payload;
import com.grafie.botjava.qq.interaction.QqInteractionHandler;
import com.grafie.botjava.qq.interaction.QqInteractionHandlerRegistry;
import com.grafie.botjava.service.GroupMessageSender;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class InteractionCreateActionTest {

    @Test
    void shouldDispatchButtonInteractionAndReplyByEventId() {
        QqInteractionHandler handler = new QqInteractionHandler() {
            @Override
            public boolean supports(com.grafie.botjava.entity.dto.interaction.InteractionCreateDto interaction) {
                return interaction.getData() != null
                        && Integer.valueOf(11).equals(interaction.getData().getType())
                        && "jx3:help".equals(interaction.getData().getResolved().path("button_data").asText());
            }

            @Override
            public BotResponse handle(com.grafie.botjava.entity.dto.interaction.InteractionCreateDto interaction) {
                return BotResponse.text("帮助内容");
            }
        };
        GroupMessageSender sender = mock(GroupMessageSender.class);
        InteractionCreateAction action = new InteractionCreateAction(
                new QqInteractionHandlerRegistry(List.of(handler)), sender);

        action.doAction(payload("jx3:help"));

        verify(sender).sendEventReply("group-1", "interaction-1", BotResponse.text("帮助内容"));
    }

    @Test
    void shouldIgnoreUnsupportedInteraction() {
        GroupMessageSender sender = mock(GroupMessageSender.class);
        InteractionCreateAction action = new InteractionCreateAction(
                new QqInteractionHandlerRegistry(List.of()), sender);

        action.doAction(payload("unknown"));

        verify(sender, never()).sendEventReply(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
    }

    private static Payload payload(String buttonData) {
        Payload payload = new Payload();
        payload.setOp(0);
        payload.setT("INTERACTION_CREATE");
        payload.setD(Map.of(
                "id", "interaction-1",
                "application_id", "app-1",
                "type", 2,
                "group_openid", "group-1",
                "chat_type", 1,
                "scene", "group",
                "group_member_openid", "account-1",
                "timestamp", "2026-07-13T16:30:00+09:00",
                "data", Map.of(
                        "name", "帮助",
                        "type", 11,
                        "resolved", Map.of("button_data", buttonData, "future_field", "保留")
                ),
                "future_field", "ignored"
        ));
        return payload;
    }
}
