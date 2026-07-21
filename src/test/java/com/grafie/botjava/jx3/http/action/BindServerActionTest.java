package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BindServerActionTest {

    @Test
    void shouldDelegateWholeGroupServerBinding() {
        GroupConfigurationService configuration = mock(GroupConfigurationService.class);
        when(configuration.bindServer("group-1", "乾坤一掷"))
                .thenReturn(GroupConfigurationService.BindServerResult.bound("乾坤一掷"));
        BindServerAction action = new BindServerAction(
                new ApiProperties(), mock(Jx3RequestUtil.class), configuration);
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setGroupOpenid("group-1");

        BotResponse response = action.doRequest(
                message, "绑定 乾坤一掷", REGEX.BindServerCalendar);

        verify(configuration).bindServer("group-1", "乾坤一掷");
        assertEquals("默认服务器设置成功，[乾坤一掷]", response.getContent());
    }

    @Test
    void shouldExplainExistingWholeGroupBinding() {
        GroupConfigurationService configuration = mock(GroupConfigurationService.class);
        when(configuration.bindServer("group-1", "梦江南"))
                .thenReturn(GroupConfigurationService.BindServerResult.existing("乾坤一掷"));
        BindServerAction action = new BindServerAction(
                new ApiProperties(), mock(Jx3RequestUtil.class), configuration);
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setGroupOpenid("group-1");

        BotResponse response = action.doRequest(message, "绑定 梦江南", REGEX.BindServerCalendar);

        assertEquals("默认服务器已存在，无法重复设置，当前设置为[乾坤一掷]", response.getContent());
    }
}
