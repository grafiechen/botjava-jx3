package com.grafie.botjava.service;

import com.grafie.botjava.entity.GroupInfo;
import com.grafie.botjava.mapper.GroupInfoMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 整群配置的唯一写入口，所有数据均按 group openid 隔离。
 */
@Service
public class GroupConfigurationService {

    private final GroupInfoMapper groupInfoMapper;

    public GroupConfigurationService(GroupInfoMapper groupInfoMapper) {
        this.groupInfoMapper = groupInfoMapper;
    }

    public Optional<String> findServer(String groupOpenId) {
        if (!hasText(groupOpenId)) {
            return Optional.empty();
        }
        GroupInfo groupInfo = groupInfoMapper.findByOpenGroupId(groupOpenId);
        return groupInfo == null ? Optional.empty() : Optional.ofNullable(clean(groupInfo.getServer()));
    }

    public BindServerResult bindServer(String groupOpenId, String server) {
        requireText(groupOpenId, "绑定区服需要 groupOpenId");
        requireText(server, "绑定区服需要服务器名称");
        String normalizedServer = server.trim();
        if (groupInfoMapper.bindServerIfEmpty(groupOpenId, normalizedServer) == 1) {
            return BindServerResult.bound(normalizedServer);
        }

        GroupInfo existing = groupInfoMapper.findByOpenGroupId(groupOpenId);
        if (existing != null && hasText(existing.getServer())) {
            return BindServerResult.existing(existing.getServer());
        }

        GroupInfo created = new GroupInfo();
        created.setOpenGroupId(groupOpenId);
        created.setServer(normalizedServer);
        try {
            groupInfoMapper.saveAndFlush(created);
            return BindServerResult.bound(normalizedServer);
        } catch (DataIntegrityViolationException race) {
            if (groupInfoMapper.bindServerIfEmpty(groupOpenId, normalizedServer) == 1) {
                return BindServerResult.bound(normalizedServer);
            }
            GroupInfo winner = groupInfoMapper.findByOpenGroupId(groupOpenId);
            if (winner != null && hasText(winner.getServer())) {
                return BindServerResult.existing(winner.getServer());
            }
            throw race;
        }
    }

    public void setEnabled(String groupOpenId, Setting setting, boolean enabled) {
        requireText(groupOpenId, "群设置需要 groupOpenId");
        if (setting == null) {
            throw new IllegalArgumentException("群设置项不能为空");
        }
        if (update(groupOpenId, setting, enabled) == 1) {
            return;
        }

        GroupInfo created = new GroupInfo();
        created.setOpenGroupId(groupOpenId);
        apply(created, setting, enabled);
        try {
            groupInfoMapper.saveAndFlush(created);
        } catch (DataIntegrityViolationException race) {
            if (update(groupOpenId, setting, enabled) != 1) {
                throw race;
            }
        }
    }

    /**
     * 同步 QQ 平台主动消息授权。该状态不能由群设置指令修改。
     */
    public void setActiveMessagesPlatformAllowed(String groupOpenId, boolean allowed) {
        requireText(groupOpenId, "同步主动消息平台授权需要 groupOpenId");
        if (groupInfoMapper.updateActiveMessagesPlatformAllowed(groupOpenId, allowed) == 1) {
            return;
        }

        GroupInfo created = new GroupInfo();
        created.setOpenGroupId(groupOpenId);
        created.setActiveMessagesPlatformAllowed(allowed);
        try {
            groupInfoMapper.saveAndFlush(created);
        } catch (DataIntegrityViolationException race) {
            if (groupInfoMapper.updateActiveMessagesPlatformAllowed(groupOpenId, allowed) != 1) {
                throw race;
            }
        }
    }

    private int update(String groupOpenId, Setting setting, boolean enabled) {
        return switch (setting) {
            case COMMANDS -> groupInfoMapper.updateCommandsEnabled(groupOpenId, enabled);
            case EXPERIMENTAL -> groupInfoMapper.updateExperimentalEnabled(groupOpenId, enabled);
            case ACTIVE_MESSAGES -> groupInfoMapper.updateActiveMessagesEnabled(groupOpenId, enabled);
        };
    }

    private void apply(GroupInfo groupInfo, Setting setting, boolean enabled) {
        switch (setting) {
            case COMMANDS -> groupInfo.setCommandsEnabled(enabled);
            case EXPERIMENTAL -> groupInfo.setExperimentalEnabled(enabled);
            case ACTIVE_MESSAGES -> groupInfo.setActiveMessagesEnabled(enabled);
        }
    }

    private void requireText(String value, String message) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String clean(String value) {
        return hasText(value) ? value.trim() : null;
    }

    public enum Setting {
        COMMANDS,
        EXPERIMENTAL,
        ACTIVE_MESSAGES
    }

    public record BindServerResult(boolean bound, String server) {
        public static BindServerResult bound(String server) {
            return new BindServerResult(true, server);
        }

        public static BindServerResult existing(String server) {
            return new BindServerResult(false, server);
        }
    }
}
