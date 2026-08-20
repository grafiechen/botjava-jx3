package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.data.trade.record.TradeRecordData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.AppearanceNameAliasService;
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

class TradeRecordActionTest {

    @Test
    void shouldImportTrustedAliasesFromSuccessfulJx3ApiResponse() {
        ApiProperties properties = new ApiProperties();
        properties.setDefaultServer("乾坤一掷");
        Jx3RequestUtil requestUtil = mock(Jx3RequestUtil.class);
        AppearanceNameAliasService aliasService = mock(AppearanceNameAliasService.class);
        RequestResult requestResult = new RequestResult();
        BaseResult<Object> baseResult = new BaseResult<>();
        TradeRecordData data = new TradeRecordData();
        data.setName("金发·因陀罗");
        data.setAlias("猴金/金发因陀罗");
        data.setView("https://static.nicemoe.cn/static/view/example.png");
        baseResult.setCode(200);
        baseResult.setData(data);

        when(aliasService.resolve("group-1", "猴金")).thenReturn("金发·因陀罗");
        when(aliasService.importTrustedAliases("金发·因陀罗", "猴金/金发因陀罗"))
                .thenReturn(new AppearanceNameAliasService.TrustedImportResult(
                        "金发·因陀罗", List.of("猴金", "金发因陀罗"), List.of(), List.of()));
        when(requestUtil.doPostRequest(anyString(), any())).thenReturn(requestResult);
        when(requestUtil.getResultRealData(requestResult, REGEX.TradeRecord.getMethodEnum()))
                .thenReturn(baseResult);

        TradeRecordAction action = new TradeRecordAction(
                properties, requestUtil, mock(GroupConfigurationService.class), aliasService);

        BotResponse response = action.doRequest(message(), "物价 猴金", REGEX.TradeRecord);

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(requestUtil).doPostRequest(
                eq(REGEX.TradeRecord.getMethodEnum().getMethodPath()), params.capture());
        assertEquals("金发·因陀罗", params.getValue().get("name"));
        verify(aliasService).importTrustedAliases("金发·因陀罗", "猴金/金发因陀罗");
        assertEquals(BotResponse.ResponseType.IMAGE, response.getResponseType());
        assertEquals("物品价格", response.getTemplateName());
    }

    private GroupAtMessageCreateDto message() {
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setId("message-1");
        message.setGroupOpenid("group-1");
        return message;
    }
}