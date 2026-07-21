package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.data.official.OfficialQueryData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BattleRecordsActionTest {

    @Test
    void shouldBuildReadableBattleViewAndKeepRequestContract() {
        ApiProperties properties = new ApiProperties();
        properties.setDefaultServer("梦江南");
        Jx3RequestUtil requestUtil = mock(Jx3RequestUtil.class);
        RequestResult requestResult = new RequestResult();
        BaseResult<Object> baseResult = new BaseResult<>();
        baseResult.setCode(200);
        baseResult.setData(List.of(record()));
        when(requestUtil.doPostRequest(anyString(), any())).thenReturn(requestResult);
        when(requestUtil.getResultRealData(requestResult, REGEX.BattleRecords.getMethodEnum()))
                .thenReturn(baseResult);
        BattleRecordsAction action = new BattleRecordsAction(
                properties, requestUtil, mock(GroupConfigurationService.class));

        BotResponse response = action.doRequest(message(), "帮战 乾坤一掷", REGEX.BattleRecords);

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(requestUtil).doPostRequest(
                eq(REGEX.BattleRecords.getMethodEnum().getMethodPath()), params.capture());
        assertEquals(Map.of("server", "乾坤一掷"), params.getValue());
        assertEquals(BotResponse.ResponseType.IMAGE, response.getResponseType());
        assertEquals("帮战记录", response.getTemplateName());
        Map<?, ?> templateData = (Map<?, ?>) response.getTemplateData();
        assertEquals("乾坤一掷", templateData.get("server"));
        List<?> views = (List<?>) templateData.get("data");
        BattleRecordsAction.BattleRecordView view =
                (BattleRecordsAction.BattleRecordView) views.get(0);
        assertEquals("1小时", view.duration());
        assertNotNull(view.startTime());
        assertNotNull(view.endTime());
    }

    private OfficialQueryData.BattleRecord record() {
        OfficialQueryData.BattleRecord record = new OfficialQueryData.BattleRecord();
        record.setZoneName("电信区");
        record.setServerName("乾坤一掷");
        record.setDeclaringTongName("醉星河");
        record.setAcceptingTongName("云上澜歌");
        record.setStartTime(1784073600L);
        record.setMatchDuration(3600L);
        record.setEndTime(1784077200L);
        return record;
    }

    private GroupAtMessageCreateDto message() {
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setId("message-1");
        message.setGroupOpenid("group-1");
        return message;
    }
}
