package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupCommandSettingService;
import com.grafie.botjava.service.GroupConfigurationService;

import java.util.Comparator;
import java.util.Map;

/**
 * 群维度查询和实验功能开关。
 */
@Jx3Action
public class GroupSettingsAction extends Jx3BaseAction {

    private final GroupCommandSettingService commandSettingService;
    private final GroupConfigurationService groupConfigurationService;

    public GroupSettingsAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil,
                               GroupCommandSettingService commandSettingService,
                               GroupConfigurationService groupConfigurationService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
        this.commandSettingService = commandSettingService;
        this.groupConfigurationService = groupConfigurationService;
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        return Map.of();
    }

    @Override
    protected BotResponse dealAfterJx3ApiRequest(BaseResult baseResult) {
        if (currentRegex() == REGEX.GroupCommandSettings) {
            return dealCommandSetting();
        }
        String setting = currentArguments().get("value");
        boolean enabled = "开启".equals(currentArguments().get("value1"));
        GroupConfigurationService.Setting groupSetting;
        if ("查询".equals(setting)) {
            groupSetting = GroupConfigurationService.Setting.COMMANDS;
        } else if ("实验".equals(setting)) {
            groupSetting = GroupConfigurationService.Setting.EXPERIMENTAL;
        } else if ("主动消息".equals(setting)) {
            groupSetting = GroupConfigurationService.Setting.ACTIVE_MESSAGES;
        } else {
            return BotResponse.text("不支持的群设置项。");
        }
        groupConfigurationService.setEnabled(currentMessage().getGroupOpenid(), groupSetting, enabled);
        return BotResponse.text(String.format("群%s功能已%s。", setting, enabled ? "开启" : "关闭"));
    }

    private BotResponse dealCommandSetting() {
        String commandName = currentArguments().get("name");
        if (commandName == null) {
            String disabled = commandSettingService.findOverrides(currentMessage().getGroupOpenid()).entrySet()
                    .stream()
                    .filter(entry -> Boolean.FALSE.equals(entry.getValue()))
                    .map(Map.Entry::getKey)
                    .sorted(Comparator.comparing(REGEX::getDisplayName))
                    .map(REGEX::getDisplayName)
                    .reduce((left, right) -> left + "、" + right)
                    .orElse("无");
            return BotResponse.text("本群已关闭的单项指令：" + disabled
                    + "\n设置方式：群指令 指令名称 开启/关闭");
        }
        REGEX definition = REGEX.findByName(commandName);
        if (definition == null) {
            return BotResponse.text("未找到指令：“" + commandName + "”，请使用帮助菜单中的指令名称。");
        }
        if (definition.getCommandAvailability() == REGEX.CommandAvailability.SYSTEM) {
            return BotResponse.text("系统指令不能通过单项设置关闭。");
        }
        boolean enabled = "开启".equals(currentArguments().get("value1"));
        commandSettingService.setEnabled(currentMessage().getGroupOpenid(), definition, enabled);
        return BotResponse.text("本群“" + definition.getDisplayName() + "”指令已"
                + (enabled ? "开启。" : "关闭。"));
    }
}
