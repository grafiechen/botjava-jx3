package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.data.luck.LuckAdventureData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LuckAdventureActionTest {

    @Test
    void shouldKeepRequestContractAndReturnImageTemplate() {
        ApiProperties properties = new ApiProperties();
        properties.setDefaultServer("梦江南");
        Jx3RequestUtil requestUtil = mock(Jx3RequestUtil.class);
        RequestResult requestResult = new RequestResult();
        BaseResult<Object> baseResult = new BaseResult<>();
        baseResult.setCode(200);
        baseResult.setData(List.of(record()));
        when(requestUtil.doPostRequest(anyString(), any())).thenReturn(requestResult);
        when(requestUtil.getResultRealData(requestResult, REGEX.LuckAdventure.getMethodEnum()))
                .thenReturn(baseResult);
        LuckAdventureAction action = new LuckAdventureAction(
                properties, requestUtil, mock(GroupConfigurationService.class));

        BotResponse response = action.doRequest(
                message(), "奇遇 乾坤一掷 测试角色", REGEX.LuckAdventure);

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(requestUtil).doPostRequest(
                eq(REGEX.LuckAdventure.getMethodEnum().getMethodPath()), params.capture());
        assertEquals(Map.of("server", "乾坤一掷", "name", "测试角色"), params.getValue());
        assertEquals(BotResponse.ResponseType.IMAGE, response.getResponseType());
        assertEquals("角色奇遇", response.getTemplateName());
        Map<?, ?> templateData = (Map<?, ?>) response.getTemplateData();
        assertEquals("乾坤一掷", templateData.get("server"));
        assertEquals("测试角色", templateData.get("name"));
    }

    private LuckAdventureData record() {
        LuckAdventureData record = new LuckAdventureData();
        record.setZone("电信区");
        record.setServer("乾坤一掷");
        record.setName("测试角色");
        record.setEvent("泛天河");
        record.setTime(1784073600L);
        return record;
    }

    private GroupAtMessageCreateDto message() {
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setId("message-1");
        message.setGroupOpenid("group-1");
        return message;
    }
}
