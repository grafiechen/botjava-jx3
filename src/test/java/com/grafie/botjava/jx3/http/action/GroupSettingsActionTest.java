package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import com.grafie.botjava.service.GroupCommandSettingService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroupSettingsActionTest {

    @Test
    void shouldDelegateQueryExperimentalAndActiveMessageSettings() {
        GroupConfigurationService configuration = mock(GroupConfigurationService.class);
        GroupSettingsAction action = action(mock(GroupCommandSettingService.class), configuration);

        BotResponse query = action.doRequest(message(), "群设置 查询 关闭", REGEX.GroupSettings);
        BotResponse experimental = action.doRequest(message(), "群设置 实验 开启", REGEX.GroupSettings);
        BotResponse active = action.doRequest(message(), "群设置 主动消息 开启", REGEX.GroupSettings);

        verify(configuration).setEnabled("group-1", GroupConfigurationService.Setting.COMMANDS, false);
        verify(configuration).setEnabled("group-1", GroupConfigurationService.Setting.EXPERIMENTAL, true);
        verify(configuration).setEnabled("group-1", GroupConfigurationService.Setting.ACTIVE_MESSAGES, true);
        assertEquals("群查询功能已关闭。", query.getContent());
        assertEquals("群实验功能已开启。", experimental.getContent());
        assertEquals("群主动消息功能已开启。", active.getContent());
    }

    @Test
    void shouldDisableOneCommandByDisplayName() {
        GroupCommandSettingService settings = mock(GroupCommandSettingService.class);

        BotResponse response = action(settings, mock(GroupConfigurationService.class)).doRequest(
                message(), "群指令 开服状态 关闭", REGEX.GroupCommandSettings);

        verify(settings).setEnabled("group-1", REGEX.ServerCheck, false);
        assertEquals("本群“开服状态”指令已关闭。", response.getContent());
    }

    @Test
    void shouldListDisabledCommandsAndRejectSystemCommand() {
        GroupCommandSettingService settings = mock(GroupCommandSettingService.class);
        when(settings.findOverrides("group-1"))
                .thenReturn(Map.of(REGEX.ServerCheck, false, REGEX.NewsAllNews, true));
        GroupSettingsAction action = action(settings, mock(GroupConfigurationService.class));

        BotResponse listed = action.doRequest(message(), "群指令", REGEX.GroupCommandSettings);
        BotResponse rejected = action.doRequest(
                message(), "群指令 帮助菜单 关闭", REGEX.GroupCommandSettings);

        assertTrue(listed.getContent().contains("开服状态"));
        assertFalse(listed.getContent().contains("新闻资讯"));
        assertEquals("系统指令不能通过单项设置关闭。", rejected.getContent());
    }

    private static GroupSettingsAction action(GroupCommandSettingService settings,
                                              GroupConfigurationService configuration) {
        return new GroupSettingsAction(new ApiProperties(), mock(Jx3RequestUtil.class),
                settings, configuration);
    }

    private static GroupAtMessageCreateDto message() {
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setId("message-1");
        message.setGroupOpenid("group-1");
        return message;
    }
}
