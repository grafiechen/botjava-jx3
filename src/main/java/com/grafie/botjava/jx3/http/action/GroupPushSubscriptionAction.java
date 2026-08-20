package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.BotAdminAuditService;
import com.grafie.botjava.service.GroupConfigurationService;
import com.grafie.botjava.service.GroupPushSubscriptionService;
import com.grafie.botjava.service.push.PushTaskDefinition;
import com.grafie.botjava.service.push.PushTaskRegistry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Jx3Action
public class GroupPushSubscriptionAction extends Jx3BaseAction {

    private final GroupPushSubscriptionService subscriptionService;
    private final PushTaskRegistry taskRegistry;
    private final BotAdminAuditService auditService;

    public GroupPushSubscriptionAction(ApiProperties apiProperties,
                                       Jx3RequestUtil jx3RequestUtil,
                                       GroupConfigurationService groupConfigurationService,
                                       GroupPushSubscriptionService subscriptionService,
                                       PushTaskRegistry taskRegistry,
                                       BotAdminAuditService auditService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
        this.subscriptionService = subscriptionService;
        this.taskRegistry = taskRegistry;
        this.auditService = auditService;
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        return Map.of();
    }

    @Override
    protected BotResponse dealAfterJx3ApiRequest(BaseResult baseResult) {
        if (currentRegex() == REGEX.GroupPushList) {
            return listSubscriptions();
        }
        boolean enabled = currentRegex() == REGEX.GroupPushEnable;
        String taskName = currentArguments().get("name");
        if (taskName == null) {
            return BotResponse.text("请提供推送任务名。");
        }
        PushTaskDefinition task = taskRegistry.find(taskName).orElse(null);
        if (task == null) {
            return BotResponse.text("未找到推送任务：“" + taskName + "”。请使用“推送列表”查看任务名。");
        }
        String groupOpenId = currentMessage().getGroupOpenid();
        String memberOpenId = currentMessage().getAuthor() == null
                ? null : currentMessage().getAuthor().getMemberOpenid();
        try {
            subscriptionService.setEnabled(groupOpenId, task, enabled, memberOpenId);
            auditService.record("GROUP_PUSH", enabled ? "ENABLE" : "DISABLE",
                    groupOpenId + ":" + task.code(), memberOpenId, "GROUP", true, null);
        } catch (RuntimeException e) {
            auditService.record("GROUP_PUSH", enabled ? "ENABLE" : "DISABLE",
                    groupOpenId + ":" + task.code(), memberOpenId, "GROUP", false,
                    e.getClass().getSimpleName());
            throw e;
        }
        String suffix = task.producerReady()
                ? "" : "（任务执行器尚未接入，接入后自动生效）";
        return BotResponse.text("本群“" + task.displayName() + "”推送已"
                + (enabled ? "开启" : "关闭") + "。" + suffix);
    }

    private BotResponse listSubscriptions() {
        Map<PushTaskDefinition, Boolean> subscriptions =
                subscriptionService.list(currentMessage().getGroupOpenid());
        List<Map<String, Object>> rows = new ArrayList<>();
        int enabledCount = 0;
        int wsCount = 0;
        for (Map.Entry<PushTaskDefinition, Boolean> entry : subscriptions.entrySet()) {
            PushTaskDefinition task = entry.getKey();
            boolean enabled = Boolean.TRUE.equals(entry.getValue());
            if (enabled) {
                enabledCount++;
            }
            if (task.source().isWebSocket()) {
                wsCount++;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("source", task.source().getDisplayName());
            row.put("sourceCode", task.source().name());
            row.put("category", task.category());
            row.put("name", task.displayName());
            row.put("code", task.code());
            row.put("enabled", enabled);
            row.put("ready", task.producerReady());
            rows.add(row);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("enabledCount", enabledCount);
        data.put("disabledCount", rows.size() - enabledCount);
        data.put("wsCount", wsCount);
        data.put("rows", rows);
        return BotResponse.image("推送列表", data);
    }
}