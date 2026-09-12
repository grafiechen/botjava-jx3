package com.grafie.botjava.service.push;

import com.grafie.botjava.entity.dto.common.BotResponse;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ScheduledGroupPushRunnerTest {

    @Test
    void shouldDelegateScheduledTaskToCommonDispatcher() {
        GroupPushDispatcher dispatcher = mock(GroupPushDispatcher.class);
        ScheduledGroupPushRunner runner = new ScheduledGroupPushRunner(dispatcher);
        PushTaskDefinition definition = new PushTaskRegistry()
                .find(PushTaskRegistry.MONGO_DAILY_PROGRESS).orElseThrow();
        ScheduledGroupPushTask task = new ScheduledGroupPushTask() {
            @Override
            public PushTaskDefinition definition() {
                return definition;
            }

            @Override
            public BotResponse buildResponse(String groupOpenId) {
                return BotResponse.text(groupOpenId);
            }
        };

        runner.run(task);

        verify(dispatcher).publish(eq(definition), isA(Function.class));
    }

    @Test
    void shouldBuildSharedScheduledResponseOnceBeforeDispatch() {
        GroupPushDispatcher dispatcher = mock(GroupPushDispatcher.class);
        ScheduledGroupPushRunner runner = new ScheduledGroupPushRunner(dispatcher);
        PushTaskDefinition definition = new PushTaskRegistry()
                .find(PushTaskRegistry.MONGO_BAG_SPACE_WARNING).orElseThrow();
        ScheduledGroupPushTask task = mock(ScheduledGroupPushTask.class);
        BotResponse response = BotResponse.text("背包预警");
        org.mockito.Mockito.when(task.definition()).thenReturn(definition);
        org.mockito.Mockito.when(task.buildResponse(null)).thenReturn(response);

        runner.runShared(task);

        verify(task).buildResponse(null);
        verify(dispatcher).publish(definition, response);
    }

    @Test
    void shouldSkipSharedDispatchWhenTaskHasNoContent() {
        GroupPushDispatcher dispatcher = mock(GroupPushDispatcher.class);
        ScheduledGroupPushRunner runner = new ScheduledGroupPushRunner(dispatcher);
        PushTaskDefinition definition = new PushTaskRegistry()
                .find(PushTaskRegistry.MONGO_BAG_SPACE_WARNING).orElseThrow();
        ScheduledGroupPushTask task = mock(ScheduledGroupPushTask.class);
        org.mockito.Mockito.when(task.definition()).thenReturn(definition);
        org.mockito.Mockito.when(task.buildResponse(null)).thenReturn(null);

        runner.runShared(task);

        verify(dispatcher, never()).publish(eq(definition), isA(BotResponse.class));
    }

    @Test
    void shouldRejectWsTaskAtScheduledEntry() {
        GroupPushDispatcher dispatcher = mock(GroupPushDispatcher.class);
        ScheduledGroupPushRunner runner = new ScheduledGroupPushRunner(dispatcher);
        PushTaskDefinition wsDefinition = new PushTaskRegistry().find("开服状态").orElseThrow();
        ScheduledGroupPushTask task = mock(ScheduledGroupPushTask.class);
        org.mockito.Mockito.when(task.definition()).thenReturn(wsDefinition);

        assertThrows(IllegalArgumentException.class, () -> runner.run(task));
    }
}