package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.data.server.ServerCheckData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

class ServerCheckActionTest {

    @Test
    void shouldRenderReadableStatusAndOpenTime() {
        Jx3RequestUtil requestUtil = mock(Jx3RequestUtil.class);
        GroupConfigurationService configurationService = mock(GroupConfigurationService.class);
        when(configurationService.findServer("group-1")).thenReturn(Optional.empty());

        RequestResult requestResult = new RequestResult();
        BaseResult<ServerCheckData> baseResult = new BaseResult<>();
        baseResult.setCode(200);
        baseResult.setTime(1723007064L);
        ServerCheckData data = new ServerCheckData();
        data.setServer("乾坤一掷");
        data.setStatus("1");
        baseResult.setData(data);

        when(requestUtil.doPostRequest(eq(REGEX.ServerCheck.getMethodEnum().getMethodPath()), anyMap())).thenReturn(requestResult);
        doReturn(baseResult).when(requestUtil).getResultRealData(requestResult, REGEX.ServerCheck.getMethodEnum());

        ApiProperties properties = new ApiProperties();
        properties.setDefaultServer("乾坤一掷");
        ServerCheckAction action = new ServerCheckAction(properties, requestUtil, configurationService);

        BotResponse response = action.doRequest(message(), "开服 乾坤一掷", REGEX.ServerCheck);

        assertEquals("服务器[乾坤一掷]：已开服，开服时间：2024-08-07 13:04:24", response.getContent());
    }

    private static GroupAtMessageCreateDto message() {
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setId("message-1");
        message.setGroupOpenid("group-1");
        return message;
    }
}
