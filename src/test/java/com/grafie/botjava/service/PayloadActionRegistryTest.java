package com.grafie.botjava.service;

import com.grafie.botjava.action.GroupAtMessageAction;
import com.grafie.botjava.action.GroupLifecycleAction;
import com.grafie.botjava.action.InteractionCreateAction;
import com.grafie.botjava.contants.PayloadTEnum;
import com.grafie.botjava.qq.group.QqGroupLifecycleHandlerRegistry;
import com.grafie.botjava.qq.interaction.QqInteractionHandlerRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class PayloadActionRegistryTest {

    @Test
    void shouldUseSameGroupHandlerForAtAndNonAtEvents() {
        GroupAtMessageAction groupAction = new GroupAtMessageAction(
                mock(GroupCommandExecutionService.class));
        InteractionCreateAction interactionAction = new InteractionCreateAction(
                mock(QqInteractionHandlerRegistry.class), mock(GroupMessageSender.class));
        GroupLifecycleAction lifecycleAction = new GroupLifecycleAction(
                mock(QqGroupLifecycleHandlerRegistry.class));
        PayloadActionRegistry registry = new PayloadActionRegistry(
                List.of(groupAction, interactionAction, lifecycleAction));

        assertSame(groupAction, registry.get(PayloadTEnum.GROUP_AT_MESSAGE_CREATE));
        assertSame(groupAction, registry.get(PayloadTEnum.GROUP_MESSAGE_CREATE));
        assertSame(interactionAction, registry.get(PayloadTEnum.INTERACTION_CREATE));
        assertSame(lifecycleAction, registry.get(PayloadTEnum.GROUP_ADD_ROBOT));
        assertSame(lifecycleAction, registry.get(PayloadTEnum.GROUP_DEL_ROBOT));
        assertSame(lifecycleAction, registry.get(PayloadTEnum.GROUP_MSG_RECEIVE));
        assertSame(lifecycleAction, registry.get(PayloadTEnum.GROUP_MSG_REJECT));
    }

    @Test
    void shouldRejectRegistryWithMissingEventHandler() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> new PayloadActionRegistry(List.of())
        );

        assertTrue(exception.getMessage().contains("缺少处理器"));
    }
}
