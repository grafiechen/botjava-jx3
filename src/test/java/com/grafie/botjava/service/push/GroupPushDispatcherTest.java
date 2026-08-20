package com.grafie.botjava.service.push;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.qq.group.QqGroupLifecycleEventType;
import com.grafie.botjava.service.GroupMessageSender;
import com.grafie.botjava.service.GroupPushSubscriptionService;
import com.grafie.botjava.service.PushEventDeduplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupPushDispatcherTest {

    @Mock
    private GroupPushSubscriptionService subscriptionService;
    @Mock
    private GroupMessageSender messageSender;
    @Mock
    private PushEventDeduplicationService deduplicationService;

    private PushTaskRegistry registry;
    private PushTaskDefinition jx3Task;
    private GroupPushDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        registry = new PushTaskRegistry();
        jx3Task = registry.find("开服状态").orElseThrow();
        TaskExecutor directExecutor = Runnable::run;
        dispatcher = new GroupPushDispatcher(
                subscriptionService, messageSender, deduplicationService, directExecutor);
    }

    @Test
    void duplicateWsEventShouldNotEnterDeliveryQueue() {
        when(deduplicationService.tryClaim(jx3Task, "a".repeat(64))).thenReturn(false);

        dispatcher.publishWsEvent(jx3Task, "a".repeat(64), BotResponse.text("开服"));

        verify(subscriptionService, never()).findEnabledGroupOpenIds(any());
        verify(messageSender, never()).sendActive(any(), any());
    }

    @Test
    void claimedJx3WsEventShouldOnlyTargetSubscribedGroups() {
        when(deduplicationService.tryClaim(jx3Task, "a".repeat(64))).thenReturn(true);
        when(subscriptionService.findEnabledGroupOpenIds(jx3Task)).thenReturn(List.of("group-a"));
        when(messageSender.sendActive("group-a", BotResponse.text("开服")))
                .thenReturn(GroupMessageSender.ActiveMessageResult.delivered());

        BotResponse response = BotResponse.text("开服");
        dispatcher.publishWsEvent(jx3Task, "a".repeat(64), response);

        verify(messageSender).sendActive("group-a", response);
        verify(messageSender, never()).sendActive(org.mockito.ArgumentMatchers.eq("group-b"), any());
    }

    @Test
    void disabledQqWsEventShouldNotBeClaimedOrSent() {
        PushTaskDefinition task = registry.findByQqWsEvent(
                QqGroupLifecycleEventType.ROBOT_ADDED).orElseThrow();
        when(subscriptionService.isEnabled("group-a", task)).thenReturn(false);

        dispatcher.publishWsEventToGroup(
                task, "group-a", "b".repeat(64), BotResponse.text("机器人加入群聊"));

        verify(deduplicationService, never()).tryClaim(any(), any());
        verify(messageSender, never()).sendActive(any(), any());
    }

    @Test
    void enabledQqWsEventShouldOnlyReturnToEventGroup() {
        PushTaskDefinition task = registry.findByQqWsEvent(
                QqGroupLifecycleEventType.PROACTIVE_MESSAGES_ACCEPTED).orElseThrow();
        BotResponse response = BotResponse.text("主动消息授权开启");
        when(subscriptionService.isEnabled("group-a", task)).thenReturn(true);
        when(deduplicationService.tryClaim(task, "c".repeat(64))).thenReturn(true);
        when(messageSender.sendActive("group-a", response))
                .thenReturn(GroupMessageSender.ActiveMessageResult.delivered());

        dispatcher.publishWsEventToGroup(task, "group-a", "c".repeat(64), response);

        verify(messageSender).sendActive("group-a", response);
        verify(messageSender, never()).sendActive(org.mockito.ArgumentMatchers.eq("group-b"), any());
    }
}