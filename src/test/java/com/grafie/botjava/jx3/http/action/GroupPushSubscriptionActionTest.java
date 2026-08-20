package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.BotAdminAuditService;
import com.grafie.botjava.service.GroupConfigurationService;
import com.grafie.botjava.service.GroupPushSubscriptionService;
import com.grafie.botjava.service.push.PushTaskDefinition;
import com.grafie.botjava.service.push.PushTaskRegistry;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GroupPushSubscriptionActionTest {

    @Test
    void listShouldReturnTableImageWithEveryEnabledAndDisabledTask() {
        GroupPushSubscriptionService subscriptionService =
                mock(GroupPushSubscriptionService.class);
        PushTaskRegistry registry = new PushTaskRegistry();
        GroupPushSubscriptionAction action = new GroupPushSubscriptionAction(
                new ApiProperties(),
                mock(Jx3RequestUtil.class),
                mock(GroupConfigurationService.class),
                subscriptionService,
                registry,
                mock(BotAdminAuditService.class));
        Map<PushTaskDefinition, Boolean> subscriptions = new LinkedHashMap<>();
        for (PushTaskDefinition task : registry.all()) {
            subscriptions.put(task, "开服状态".equals(task.displayName())
                    || "主动消息授权开启".equals(task.displayName()));
        }
        when(subscriptionService.list("group-a")).thenReturn(subscriptions);
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setGroupOpenid("group-a");

        BotResponse response = action.doRequest(message, "推送列表", REGEX.GroupPushList);

        assertThat(response.getResponseType()).isEqualTo(BotResponse.ResponseType.IMAGE);
        assertThat(response.getTemplateName()).isEqualTo("推送列表");
        assertThat(response.getTemplateData()).isInstanceOf(Map.class);
        Map<?, ?> data = (Map<?, ?>) response.getTemplateData();
        assertThat(data.get("enabledCount")).isEqualTo(2);
        assertThat(data.get("disabledCount")).isEqualTo(registry.all().size() - 2);
        assertThat((java.util.List<?>) data.get("rows")).hasSize(registry.all().size());
        assertThat((java.util.List<?>) data.get("rows"))
                .anySatisfy(row -> {
                    Map<?, ?> item = (Map<?, ?>) row;
                    assertThat(item.get("source")).isEqualTo("QQ WebSocket");
                    assertThat(item.get("name")).isEqualTo("主动消息授权开启");
                    assertThat(item.get("enabled")).isEqualTo(true);
                })
                .anySatisfy(row -> {
                    Map<?, ?> item = (Map<?, ?>) row;
                    assertThat(item.get("source")).isEqualTo("JX3API WebSocket");
                    assertThat(item.get("name")).isEqualTo("开服状态");
                    assertThat(item.get("enabled")).isEqualTo(true);
                });
    }
}