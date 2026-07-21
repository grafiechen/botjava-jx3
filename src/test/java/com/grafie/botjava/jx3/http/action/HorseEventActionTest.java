package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.data.horse.HorseRanchData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HorseEventActionTest {

    @Test
    void shouldConvertDynamicMapKeysToStableImageRows() {
        HorseRanchData ranch = new HorseRanchData();
        ranch.setZone("电信区");
        ranch.setServer("梦江南");
        ranch.setNote("数据仅供参考，请以游戏内为准。");
        Map<String, List<String>> predictions = new LinkedHashMap<>();
        predictions.put("黑戈壁", List.of("时间尚久，无法预知。"));
        predictions.put("龙泉府 / 进图（21:10）", List.of("的卢（07/15 21:15）"));
        ranch.setData(predictions);

        Jx3RequestUtil requestUtil = mock(Jx3RequestUtil.class);
        RequestResult requestResult = new RequestResult();
        BaseResult<Object> baseResult = new BaseResult<>();
        baseResult.setCode(200);
        baseResult.setData(ranch);
        when(requestUtil.doPostRequest(anyString(), any())).thenReturn(requestResult);
        when(requestUtil.getResultRealData(requestResult, REGEX.HorseEvent.getMethodEnum()))
                .thenReturn(baseResult);
        ApiProperties properties = new ApiProperties();
        properties.setDefaultServer("乾坤一掷");
        HorseEventAction action = new HorseEventAction(
                properties, requestUtil, mock(GroupConfigurationService.class));

        BotResponse response = action.doRequest(message(), "马场 梦江南", REGEX.HorseEvent);

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(requestUtil).doPostRequest(
                eq(REGEX.HorseEvent.getMethodEnum().getMethodPath()), params.capture());
        assertEquals(Map.of("server", "梦江南"), params.getValue());
        assertEquals(BotResponse.ResponseType.IMAGE, response.getResponseType());
        assertEquals("马场刷新", response.getTemplateName());
        Map<?, ?> templateData = (Map<?, ?>) response.getTemplateData();
        assertEquals("电信区", templateData.get("zone"));
        assertEquals("梦江南", templateData.get("server"));
        assertEquals("数据仅供参考，请以游戏内为准。", templateData.get("note"));
        List<?> rows = (List<?>) templateData.get("data");
        assertEquals(2, rows.size());
        HorseEventAction.RanchPredictionView first =
                (HorseEventAction.RanchPredictionView) rows.get(0);
        assertEquals("黑戈壁", first.mapName());
        assertEquals(List.of("时间尚久，无法预知。"), first.predictions());
    }

    private GroupAtMessageCreateDto message() {
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setId("message-1");
        message.setGroupOpenid("group-1");
        return message;
    }
}
