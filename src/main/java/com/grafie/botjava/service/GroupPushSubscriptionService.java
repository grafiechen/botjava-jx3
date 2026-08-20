package com.grafie.botjava.service;

import com.grafie.botjava.entity.GroupPushSubscription;
import com.grafie.botjava.mapper.GroupPushSubscriptionMapper;
import com.grafie.botjava.service.push.PushTaskDefinition;
import com.grafie.botjava.service.push.PushTaskRegistry;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 群推送订阅的唯一写入口。未持久化的任务默认关闭。
 */
@Service
public class GroupPushSubscriptionService {

    private final GroupPushSubscriptionMapper mapper;
    private final PushTaskRegistry registry;

    public GroupPushSubscriptionService(GroupPushSubscriptionMapper mapper,
                                        PushTaskRegistry registry) {
        this.mapper = mapper;
        this.registry = registry;
    }

    public Map<PushTaskDefinition, Boolean> list(String groupOpenId) {
        requireText(groupOpenId, "查看推送订阅需要 groupOpenId");
        Map<String, Boolean> overrides = new LinkedHashMap<>();
        for (GroupPushSubscription subscription : mapper.findByGroupOpenIdOrderByTaskCodeAsc(groupOpenId)) {
            overrides.put(subscription.getTaskCode(), Boolean.TRUE.equals(subscription.getEnabled()));
        }
        Map<PushTaskDefinition, Boolean> result = new LinkedHashMap<>();
        for (PushTaskDefinition definition : registry.all()) {
            result.put(definition, Boolean.TRUE.equals(overrides.get(definition.code())));
        }
        return result;
    }

    public void setEnabled(String groupOpenId, PushTaskDefinition task,
                           boolean enabled, String updatedBy) {
        requireText(groupOpenId, "设置推送订阅需要 groupOpenId");
        if (task == null || registry.find(task.code()).isEmpty()) {
            throw new IllegalArgumentException("推送任务不存在");
        }
        String actor = clean(updatedBy);
        if (mapper.updateEnabled(groupOpenId, task.code(), enabled, actor) == 1) {
            return;
        }

        GroupPushSubscription created = new GroupPushSubscription();
        created.setGroupOpenId(groupOpenId.trim());
        created.setTaskCode(task.code());
        created.setEnabled(enabled);
        created.setUpdatedBy(actor);
        try {
            mapper.saveAndFlush(created);
        } catch (DataIntegrityViolationException race) {
            if (mapper.updateEnabled(groupOpenId, task.code(), enabled, actor) != 1) {
                throw race;
            }
        }
    }

    public boolean isEnabled(String groupOpenId, PushTaskDefinition task) {
        if (groupOpenId == null || groupOpenId.isBlank() || task == null) {
            return false;
        }
        GroupPushSubscription subscription = mapper.findByGroupOpenIdAndTaskCode(
                groupOpenId.trim(), task.code());
        return subscription != null && Boolean.TRUE.equals(subscription.getEnabled());
    }

    public List<String> findEnabledGroupOpenIds(PushTaskDefinition task) {
        if (task == null) {
            return List.of();
        }
        return mapper.findEnabledGroupOpenIds(task.code());
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
