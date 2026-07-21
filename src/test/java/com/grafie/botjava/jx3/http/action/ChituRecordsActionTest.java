package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.data.official.OfficialQueryData;
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

class ChituRecordsActionTest {

    @Test
    void shouldBuildDailyImageWithReadableFieldsOnly() {
        OfficialQueryData.ChituRecord record = new OfficialQueryData.ChituRecord();
        record.setId(21L);
        record.setServer("破阵子");
        record.setMapName("黑戈壁");
        record.setHorse("赤兔");
        record.setSend(1);
        record.setDate("2026-07-15");

        Jx3RequestUtil requestUtil = requestUtil(REGEX.ChituRecords, List.of(record));
        BotResponse response = request(
                new ChituRecordsAction(properties(), requestUtil,
                        mock(GroupConfigurationService.class)),
                requestUtil, REGEX.ChituRecords, "本日赤兔");

        Map<?, ?> data = (Map<?, ?>) response.getTemplateData();
        assertEquals("本日", data.get("mode"));
        ChituRecordsAction.ChituRecordView view =
                (ChituRecordsAction.ChituRecordView) ((List<?>) data.get("data")).get(0);
        assertEquals("破阵子", view.server());
        assertEquals("黑戈壁", view.mapName());
        assertEquals("赤兔", view.horse());
        assertEquals("2026-07-15", view.date());
    }

    @Test
    void shouldBuildWeeklyImageWithReadableFieldsOnly() {
        OfficialQueryData.ChituWeekRecord record = new OfficialQueryData.ChituWeekRecord();
        record.setServer("剑胆琴心");
        record.setMapName("阴山大草原");
        record.setHorse("赤兔");
        record.setDate("2026-07-14");

        Jx3RequestUtil requestUtil = requestUtil(REGEX.ChituWeekRecords, List.of(record));
        BotResponse response = request(
                new ChituWeekRecordsAction(properties(), requestUtil,
                        mock(GroupConfigurationService.class)),
                requestUtil, REGEX.ChituWeekRecords, "本周赤兔");

        Map<?, ?> data = (Map<?, ?>) response.getTemplateData();
        assertEquals("本周", data.get("mode"));
        ChituWeekRecordsAction.ChituWeekRecordView view =
                (ChituWeekRecordsAction.ChituWeekRecordView) ((List<?>) data.get("data")).get(0);
        assertEquals("剑胆琴心", view.server());
        assertEquals("阴山大草原", view.mapName());
        assertEquals("赤兔", view.horse());
        assertEquals("2026-07-14", view.date());
    }

    private BotResponse request(Jx3BaseAction action, Jx3RequestUtil requestUtil,
                                REGEX regex, String command) {
        BotResponse response = action.doRequest(message(), command, regex);

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(requestUtil).doPostRequest(eq(regex.getMethodEnum().getMethodPath()), params.capture());
        assertEquals(Map.of(), params.getValue());
        assertEquals(BotResponse.ResponseType.IMAGE, response.getResponseType());
        assertEquals("赤兔记录", response.getTemplateName());
        assertEquals("全服", ((Map<?, ?>) response.getTemplateData()).get("server"));
        return response;
    }

    private Jx3RequestUtil requestUtil(REGEX regex, Object apiData) {
        Jx3RequestUtil requestUtil = mock(Jx3RequestUtil.class);
        RequestResult requestResult = new RequestResult();
        BaseResult<Object> baseResult = new BaseResult<>();
        baseResult.setCode(200);
        baseResult.setData(apiData);
        when(requestUtil.doPostRequest(anyString(), any())).thenReturn(requestResult);
        when(requestUtil.getResultRealData(requestResult, regex.getMethodEnum())).thenReturn(baseResult);
        return requestUtil;
    }

    private ApiProperties properties() {
        ApiProperties properties = new ApiProperties();
        properties.setDefaultServer("梦江南");
        return properties;
    }

    private GroupAtMessageCreateDto message() {
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setId("message-1");
        message.setGroupOpenid("group-1");
        return message;
    }
}
