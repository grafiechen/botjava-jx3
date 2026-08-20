package com.grafie.botjava.service.push;

import com.grafie.botjava.jx3.ws.data.Jx3WsEventEnum;
import com.grafie.botjava.qq.group.QqGroupLifecycleEventType;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

class PushTaskRegistryTest {

    private final PushTaskRegistry registry = new PushTaskRegistry();

    @Test
    void shouldRegisterQqAndJx3WsEventsAndScheduledTask() {
        assertThat(registry.all())
                .filteredOn(task -> task.source() == PushTaskSource.QQ_WS)
                .hasSize(QqGroupLifecycleEventType.values().length)
                .allMatch(PushTaskDefinition::producerReady);
        assertThat(registry.all())
                .filteredOn(task -> task.source() == PushTaskSource.JX3API_WS)
                .hasSize(Jx3WsEventEnum.values().length)
                .allMatch(PushTaskDefinition::producerReady);
        assertThat(registry.find(PushTaskRegistry.MONGO_DAILY_PROGRESS))
                .get()
                .matches(task -> task.source() == PushTaskSource.SCHEDULED)
                .matches(PushTaskDefinition::producerReady);
    }

    @Test
    void codesAndCommandNamesShouldBeUnique() {
        assertThat(registry.all().stream().map(PushTaskDefinition::code).toList())
                .doesNotHaveDuplicates();
        assertThat(registry.all().stream().map(PushTaskDefinition::displayName).toList())
                .doesNotHaveDuplicates();
        assertThat(new HashSet<>(registry.all())).hasSameSizeAs(registry.all());
    }

    @Test
    void shouldResolveJx3WsTaskByActionAndChineseName() {
        PushTaskDefinition task = registry.findByWsAction(2001).orElseThrow();
        assertThat(task.displayName()).isEqualTo("开服状态");
        assertThat(task.source()).isEqualTo(PushTaskSource.JX3API_WS);
        assertThat(registry.find("开服状态")).contains(task);
        assertThat(registry.find("jx3_ws_2001")).contains(task);
    }

    @Test
    void shouldResolveQqWsTaskByEventAndChineseName() {
        PushTaskDefinition task = registry.findByQqWsEvent(
                QqGroupLifecycleEventType.PROACTIVE_MESSAGES_ACCEPTED).orElseThrow();

        assertThat(task.displayName()).isEqualTo("主动消息授权开启");
        assertThat(task.source()).isEqualTo(PushTaskSource.QQ_WS);
        assertThat(registry.find("主动消息授权开启")).contains(task);
        assertThat(registry.find("qq_ws_group_msg_receive")).contains(task);
    }
}