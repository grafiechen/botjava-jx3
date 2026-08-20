package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.data.official.OfficialQueryData;
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

class TradeRecordsActionTest {

    @Test
    void shouldKeepRequestContractAndReturnSharedPriceTemplate() {
        ApiProperties properties = new ApiProperties();
        properties.setDefaultServer("梦江南");
        Jx3RequestUtil requestUtil = mock(Jx3RequestUtil.class);
        RequestResult requestResult = new RequestResult();
        BaseResult<Object> baseResult = new BaseResult<>();
        baseResult.setCode(200);
        baseResult.setData(marketData());
        when(requestUtil.doPostRequest(anyString(), any())).thenReturn(requestResult);
        when(requestUtil.getResultRealData(requestResult, REGEX.TradeRecords.getMethodEnum()))
                .thenReturn(baseResult);
        TradeRecordsAction action = new TradeRecordsAction(
                properties, requestUtil, mock(GroupConfigurationService.class));

        BotResponse response = action.doRequest(
                message(), "黑市物价 乾坤一掷 狐金", REGEX.TradeRecords);

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(requestUtil).doPostRequest(
                eq(REGEX.TradeRecords.getMethodEnum().getMethodPath()), params.capture());
        assertEquals(Map.of("server", "乾坤一掷", "name", "狐金"), params.getValue());
        assertEquals(BotResponse.ResponseType.IMAGE, response.getResponseType());
        assertEquals("物品价格", response.getTemplateName());
        Map<?, ?> templateData = (Map<?, ?>) response.getTemplateData();
        assertEquals("黑市物价", templateData.get("mode"));
        assertEquals("乾坤一掷", templateData.get("server"));
        assertEquals("狐金", templateData.get("name"));
    }


    @Test
    void shouldResolveAppearanceNameAliasBeforeRequestingJx3Api() {
        ApiProperties properties = new ApiProperties();
        properties.setDefaultServer("梦江南");
        Jx3RequestUtil requestUtil = mock(Jx3RequestUtil.class);
        AppearanceNameAliasService aliasService = mock(AppearanceNameAliasService.class);
        RequestResult requestResult = new RequestResult();
        BaseResult<Object> baseResult = new BaseResult<>();
        baseResult.setCode(200);
        baseResult.setData(marketData());
        when(aliasService.resolve("group-1", "狐金")).thenReturn("金发·璨月蝶心");
        when(aliasService.importTrustedAliases("金发·璨月蝶心", "狐金"))
                .thenReturn(new AppearanceNameAliasService.TrustedImportResult(
                        "金发·璨月蝶心", List.of("狐金"), List.of(), List.of()));
        when(requestUtil.doPostRequest(anyString(), any())).thenReturn(requestResult);
        when(requestUtil.getResultRealData(requestResult, REGEX.TradeRecords.getMethodEnum()))
                .thenReturn(baseResult);
        TradeRecordsAction action = new TradeRecordsAction(
                properties, requestUtil, mock(GroupConfigurationService.class), aliasService);

        BotResponse response = action.doRequest(
                message(), "黑市物价 乾坤一掷 狐金", REGEX.TradeRecords);

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(requestUtil).doPostRequest(
                eq(REGEX.TradeRecords.getMethodEnum().getMethodPath()), params.capture());
        assertEquals("金发·璨月蝶心", params.getValue().get("name"));
        assertEquals("金发·璨月蝶心", ((Map<?, ?>) response.getTemplateData()).get("name"));
        verify(aliasService).importTrustedAliases("金发·璨月蝶心", "狐金");
    }
    private OfficialQueryData.TradeRecords marketData() {
        OfficialQueryData.TradeListing listing = new OfficialQueryData.TradeListing();
        listing.setZone("电信区");
        listing.setServer("乾坤一掷");
        listing.setValue(23500L);
        listing.setSale(4);
        listing.setDate("2026-07-15");
        OfficialQueryData.TradeRecords data = new OfficialQueryData.TradeRecords();
        data.setCategory("发型");
        data.setSubclass("金发");
        data.setName("金发·璨月蝶心");
        data.setAlias("狐金");
        data.setList(List.of(List.of(listing)));
        return data;
    }

    private GroupAtMessageCreateDto message() {
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setId("message-1");
        message.setGroupOpenid("group-1");
        return message;
    }
}
