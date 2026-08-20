package com.grafie.botjava.jx3.http.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class CommandListActionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldMatchOnlyExactCommandText() {
        assertEquals(REGEX.CommandList, REGEX.matchEnum("指令"));
        assertEquals(REGEX.CommandList, REGEX.matchEnum("/指令"));
        assertEquals(null, REGEX.matchEnum("指令 开服"));
        assertEquals(null, REGEX.matchEnum("所有指令"));
    }

    @Test
    void shouldBuildImageResponseFromRegisteredCommandDefinitions() {
        CommandListAction action = new CommandListAction(
                new ApiProperties(), mock(Jx3RequestUtil.class), mock(GroupConfigurationService.class));

        BotResponse response = action.doRequest(message(), "指令", REGEX.CommandList);

        assertEquals(BotResponse.ResponseType.IMAGE, response.getResponseType());
        assertEquals("指令列表", response.getTemplateName());

        Map<String, Object> data = objectMapper.convertValue(response.getTemplateData(), Map.class);
        long shownCommands = Arrays.stream(REGEX.values())
                .filter(REGEX::isShownInCommandList)
                .count();
        assertEquals((int) shownCommands, data.get("total"));
        String serializedData = data.toString();
        assertTrue(serializedData.contains("开服状态"));
        assertTrue(serializedData.contains("创建需求"));
        assertTrue(serializedData.contains("添加角色"));
        assertFalse(serializedData.contains("绑定门派"));
        assertFalse(serializedData.contains("切换角色"));
        assertFalse(serializedData.contains("(?<"));
        assertFalse(serializedData.contains("^"));

        List<?> groups = (List<?>) data.get("groups");
        assertFalse(groups.isEmpty());
    }

    private GroupAtMessageCreateDto message() {
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setGroupOpenid("group-1");
        return message;
    }
}
