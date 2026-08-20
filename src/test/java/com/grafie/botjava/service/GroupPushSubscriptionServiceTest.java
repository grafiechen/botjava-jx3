package com.grafie.botjava.service;

import com.grafie.botjava.entity.GroupPushSubscription;
import com.grafie.botjava.mapper.GroupPushSubscriptionMapper;
import com.grafie.botjava.service.push.PushTaskDefinition;
import com.grafie.botjava.service.push.PushTaskRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupPushSubscriptionServiceTest {

    @Mock
    private GroupPushSubscriptionMapper mapper;

    private PushTaskRegistry registry;
    private GroupPushSubscriptionService service;

    @BeforeEach
    void setUp() {
        registry = new PushTaskRegistry();
        service = new GroupPushSubscriptionService(mapper, registry);
    }

    @Test
    void missingRowsShouldBeDisabledByDefault() {
        when(mapper.findByGroupOpenIdOrderByTaskCodeAsc("group-a")).thenReturn(List.of());

        assertThat(service.list("group-a"))
                .hasSize(registry.all().size())
                .allSatisfy((task, enabled) -> assertThat(enabled).isFalse());
    }

    @Test
    void listShouldRemainIsolatedByGroup() {
        PushTaskDefinition task = registry.find("开服状态").orElseThrow();
        GroupPushSubscription enabled = new GroupPushSubscription();
        enabled.setTaskCode(task.code());
        enabled.setEnabled(true);
        when(mapper.findByGroupOpenIdOrderByTaskCodeAsc("group-a")).thenReturn(List.of(enabled));
        when(mapper.findByGroupOpenIdOrderByTaskCodeAsc("group-b")).thenReturn(List.of());

        assertThat(service.list("group-a").get(task)).isTrue();
        assertThat(service.list("group-b").get(task)).isFalse();
    }

    @Test
    void setShouldInsertWhenNoRowExists() {
        PushTaskDefinition task = registry.find("开服状态").orElseThrow();
        when(mapper.updateEnabled("group-a", task.code(), true, "member-a")).thenReturn(0);
        when(mapper.saveAndFlush(any(GroupPushSubscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.setEnabled("group-a", task, true, "member-a");

        verify(mapper).saveAndFlush(any(GroupPushSubscription.class));
    }
}