package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.data.horse.HorseRecordsData;
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

class HorseRecordsActionTest {

    @Test
    void shouldBuildReadableSteedImageWithoutInternalId() {
        HorseRecordsData record = new HorseRecordsData();
        record.setId(914);
        record.setZone("电信区");
        record.setServer("长安城");
        record.setMapName("龙泉府");
        record.setRefreshTime(1733490900L);
        record.setCaptureRoleName("慕深深");
        record.setCaptureCampName("恶人谷");
        record.setCaptureTime(1733492702L);
        record.setAuctionRoleName("郭千千");
        record.setAuctionCampName("恶人谷");
        record.setAuctionTime(1733494600L);
        record.setAuctionAmount("805万1514金");
        record.setStartTime(1733094000L);
        record.setEndTime(1733612400L);

        Jx3RequestUtil requestUtil = mock(Jx3RequestUtil.class);
        RequestResult requestResult = new RequestResult();
        BaseResult<Object> baseResult = new BaseResult<>();
        baseResult.setCode(200);
        baseResult.setData(List.of(record));
        when(requestUtil.doPostRequest(anyString(), any())).thenReturn(requestResult);
        when(requestUtil.getResultRealData(requestResult, REGEX.HorseRecords.getMethodEnum()))
                .thenReturn(baseResult);
        ApiProperties properties = new ApiProperties();
        properties.setDefaultServer("梦江南");
        HorseRecordsAction action = new HorseRecordsAction(
                properties, requestUtil, mock(GroupConfigurationService.class));

        BotResponse response = action.doRequest(message(), "的卢 长安城", REGEX.HorseRecords);

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(requestUtil).doPostRequest(
                eq(REGEX.HorseRecords.getMethodEnum().getMethodPath()), params.capture());
        assertEquals(Map.of("server", "长安城"), params.getValue());
        assertEquals(BotResponse.ResponseType.IMAGE, response.getResponseType());
        assertEquals("的卢记录", response.getTemplateName());
        Map<?, ?> templateData = (Map<?, ?>) response.getTemplateData();
        assertEquals("长安城", templateData.get("server"));
        HorseRecordsAction.SteedRecordView view =
                (HorseRecordsAction.SteedRecordView) ((List<?>) templateData.get("data")).get(0);
        assertEquals("2024-12-06 21:15:00", view.refreshTime());
        assertEquals("慕深深", view.captureRoleName());
        assertEquals("郭千千", view.auctionRoleName());
        assertEquals("805万1514金", view.auctionAmount());
    }

    private GroupAtMessageCreateDto message() {
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setId("message-1");
        message.setGroupOpenid("group-1");
        return message;
    }
}
