package com.grafie.botjava.service.push;

import com.grafie.botjava.entity.dto.common.BotResponse;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
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
    void shouldRejectWsTaskAtScheduledEntry() {
        GroupPushDispatcher dispatcher = mock(GroupPushDispatcher.class);
        ScheduledGroupPushRunner runner = new ScheduledGroupPushRunner(dispatcher);
        PushTaskDefinition wsDefinition = new PushTaskRegistry().find("开服状态").orElseThrow();
        ScheduledGroupPushTask task = mock(ScheduledGroupPushTask.class);
        org.mockito.Mockito.when(task.definition()).thenReturn(wsDefinition);

        assertThrows(IllegalArgumentException.class, () -> runner.run(task));
    }
}