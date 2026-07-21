package com.grafie.botjava.service;

import com.grafie.botjava.config.CommandReleaseProperties;
import com.grafie.botjava.entity.GroupInfo;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.mapper.GroupInfoMapper;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 根据群配置和指令发布阶段判断是否允许执行。
 */
@Service
public class GroupCommandPolicy {

    private final GroupInfoMapper groupInfoMapper;
    private final CommandReleaseProperties releaseProperties;
    private final GroupCommandSettingService commandSettingService;

    public GroupCommandPolicy(GroupInfoMapper groupInfoMapper, CommandReleaseProperties releaseProperties,
                              GroupCommandSettingService commandSettingService) {
        this.groupInfoMapper = groupInfoMapper;
        this.releaseProperties = releaseProperties;
        this.commandSettingService = commandSettingService;
    }

    public Decision evaluate(String groupOpenId, REGEX definition) {
        return evaluate(groupInfoMapper.findByOpenGroupId(groupOpenId), definition,
                commandSettingService.findOverrides(groupOpenId));
    }

    public List<REGEX> availableDefinitions(String groupOpenId) {
        GroupInfo groupInfo = groupInfoMapper.findByOpenGroupId(groupOpenId);
        Map<REGEX, Boolean> overrides = commandSettingService.findOverrides(groupOpenId);
        return Arrays.stream(REGEX.values())
                .filter(definition -> evaluate(groupInfo, definition, overrides).allowed())
                .toList();
    }

    private Decision evaluate(GroupInfo groupInfo, REGEX definition, Map<REGEX, Boolean> overrides) {
        if (definition.getCommandAvailability() == REGEX.CommandAvailability.SYSTEM) {
            return Decision.allow();
        }
        if (!releaseProperties.allows(definition.getCommandAvailability())) {
            return Decision.deny(switch (definition.getCommandAvailability()) {
                case EXPERIMENTAL -> "当前运行环境未开放实验指令。";
                case REVIEW -> "当前运行环境未开放审核指令。";
                default -> "当前运行环境未开放该指令。";
            });
        }
        boolean commandsEnabled = groupInfo == null || groupInfo.getCommandsEnabled() == null
                || groupInfo.getCommandsEnabled();
        if (!commandsEnabled) {
            return Decision.deny("本群已关闭查询功能，请联系群主或管理员开启。");
        }
        if (Boolean.FALSE.equals(overrides.get(definition))) {
            return Decision.deny("本群已关闭“" + definition.getDisplayName() + "”指令。");
        }
        if (definition.getCommandAvailability() == REGEX.CommandAvailability.EXPERIMENTAL) {
            boolean experimentalEnabled = groupInfo != null && Boolean.TRUE.equals(groupInfo.getExperimentalEnabled());
            if (!experimentalEnabled) {
                return Decision.deny("本群未开启实验功能。");
            }
        }
        return Decision.allow();
    }

    public record Decision(boolean allowed, String message) {
        public static Decision allow() {
            return new Decision(true, null);
        }

        public static Decision deny(String message) {
            return new Decision(false, message);
        }
    }
}
