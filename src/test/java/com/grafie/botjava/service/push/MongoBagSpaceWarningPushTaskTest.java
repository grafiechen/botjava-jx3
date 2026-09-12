package com.grafie.botjava.service.push;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.service.ScriptStatusService;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MongoBagSpaceWarningPushTaskTest {

    @Test
    void shouldUseSystemTimeAtNineAndSharedDispatcher() throws Exception {
        PushTaskRegistry registry = new PushTaskRegistry();
        ScheduledGroupPushRunner runner = mock(ScheduledGroupPushRunner.class);
        ScriptStatusService service = mock(ScriptStatusService.class);
        MongoBagSpaceWarningPushTask task = new MongoBagSpaceWarningPushTask(registry, runner, service);
        Method method = MongoBagSpaceWarningPushTask.class.getMethod("publishAtNine");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        task.publishAtNine();

        assertThat(task.definition().displayName()).isEqualTo("定时背包预警");
        assertThat(scheduled.cron()).isEqualTo("${bot.push.mongo-bag-warning.cron:0 0 9 * * *}");
        assertThat(scheduled.zone()).isEmpty();
        verify(runner).runShared(task);
    }

    @Test
    void shouldReuseInteractiveBagWarningDataBuilder() {
        PushTaskRegistry registry = new PushTaskRegistry();
        ScheduledGroupPushRunner runner = mock(ScheduledGroupPushRunner.class);
        ScriptStatusService service = mock(ScriptStatusService.class);
        MongoBagSpaceWarningPushTask task = new MongoBagSpaceWarningPushTask(registry, runner, service);
        BotResponse expected = BotResponse.image("背包预警", java.util.Map.of("count", 1));
        when(service.buildBagSpaceWarningPush()).thenReturn(expected);

        BotResponse actual = task.buildResponse(null);

        assertThat(actual).isSameAs(expected);
        verify(service).buildBagSpaceWarningPush();
    }
}