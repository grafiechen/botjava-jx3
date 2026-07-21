package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.config.CommandReleaseProperties;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.mapper.GroupInfoMapper;
import com.grafie.botjava.service.GroupConfigurationService;
import com.grafie.botjava.service.GroupCommandPolicy;
import com.grafie.botjava.service.GroupCommandSettingService;
import com.grafie.botjava.service.HelpMenuService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class HelpActionTest {

    @Test
    void shouldReturnGeneratedTextHelpWithoutCallingExternalApi() {
        GroupInfoMapper mapper = mock(GroupInfoMapper.class);
        GroupConfigurationService configuration = mock(GroupConfigurationService.class);
        GroupCommandPolicy commandPolicy = new GroupCommandPolicy(
                mapper, new CommandReleaseProperties(), mock(GroupCommandSettingService.class));
        Jx3BaseAction action = new HelpAction(
                new ApiProperties(),
                mock(Jx3RequestUtil.class),
                configuration,
                new HelpMenuService(commandPolicy)
        );
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setId("message-help");
        message.setGroupOpenid("group-help");

        BotResponse response = action.doRequest(message, "/帮助", REGEX.Help);

        assertEquals(BotResponse.ResponseType.TEXT, response.getResponseType());
        assertTrue(response.getContent().contains("可用指令"));
        assertTrue(response.getContent().contains("绑定区服"));
        assertTrue(response.getContent().contains("物品价格"));
        assertFalse(response.getContent().contains("DPS 计算"));
    }

    @Test
    void shouldReturnMarkdownKeyboardOnlyForMenuCommand() {
        GroupInfoMapper mapper = mock(GroupInfoMapper.class);
        GroupConfigurationService configuration = mock(GroupConfigurationService.class);
        GroupCommandPolicy commandPolicy = new GroupCommandPolicy(
                mapper, new CommandReleaseProperties(), mock(GroupCommandSettingService.class));
        Jx3BaseAction action = new HelpAction(
                new ApiProperties(), mock(Jx3RequestUtil.class), configuration,
                new HelpMenuService(commandPolicy));
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setGroupOpenid("group-help");

        BotResponse response = action.doRequest(message, "/菜单", REGEX.Help);

        assertEquals(BotResponse.ResponseType.MARKDOWN, response.getResponseType());
        assertEquals(2, response.getKeyboard().getContent().getRows().size());
        assertEquals("jx3:help:BASIC", response.getKeyboard().getContent().getRows()
                .get(0).getButtons().get(0).getAction().getData());
    }
}
