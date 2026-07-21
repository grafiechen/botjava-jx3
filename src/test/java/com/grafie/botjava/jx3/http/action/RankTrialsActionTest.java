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

class RankTrialsActionTest {

    @Test
    void shouldKeepRequestContractAndReturnImageTemplate() {
        ApiProperties properties = new ApiProperties();
        properties.setDefaultServer("梦江南");
        Jx3RequestUtil requestUtil = mock(Jx3RequestUtil.class);
        RequestResult requestResult = new RequestResult();
        BaseResult<Object> baseResult = new BaseResult<>();
        baseResult.setCode(200);
        baseResult.setData(ranking());
        when(requestUtil.doPostRequest(anyString(), any())).thenReturn(requestResult);
        when(requestUtil.getResultRealData(requestResult, REGEX.RankTrials.getMethodEnum()))
                .thenReturn(baseResult);
        RankTrialsAction action = new RankTrialsAction(
                properties, requestUtil, mock(GroupConfigurationService.class));

        BotResponse response = action.doRequest(message(), "试炼 乾坤一掷 花间游", REGEX.RankTrials);

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(requestUtil).doPostRequest(
                eq(REGEX.RankTrials.getMethodEnum().getMethodPath()), params.capture());
        assertEquals(Map.of("server", "乾坤一掷", "name", "花间游"), params.getValue());
        assertEquals(BotResponse.ResponseType.IMAGE, response.getResponseType());
        assertEquals("试炼排行", response.getTemplateName());
        Map<?, ?> templateData = (Map<?, ?>) response.getTemplateData();
        assertEquals("乾坤一掷", templateData.get("server"));
        assertEquals("花间游", templateData.get("name"));
        assertNotNull(templateData.get("time"));
    }

    @Test
    void shouldUseDefaultServerForSingleArgumentCommand() {
        ApiProperties properties = new ApiProperties();
        properties.setDefaultServer("梦江南");
        Jx3RequestUtil requestUtil = mock(Jx3RequestUtil.class);
        RequestResult requestResult = new RequestResult();
        BaseResult<Object> baseResult = new BaseResult<>();
        baseResult.setCode(200);
        baseResult.setData(ranking());
        when(requestUtil.doPostRequest(anyString(), any())).thenReturn(requestResult);
        when(requestUtil.getResultRealData(requestResult, REGEX.RankTrials.getMethodEnum()))
                .thenReturn(baseResult);
        RankTrialsAction action = new RankTrialsAction(
                properties, requestUtil, mock(GroupConfigurationService.class));

        action.doRequest(message(), "试炼 花间游", REGEX.RankTrials);

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(requestUtil).doPostRequest(
                eq(REGEX.RankTrials.getMethodEnum().getMethodPath()), params.capture());
        assertEquals(Map.of("server", "梦江南", "name", "花间游"), params.getValue());
    }

    private OfficialQueryData.TrialRank ranking() {
        OfficialQueryData.TrialRankEntry entry = new OfficialQueryData.TrialRankEntry();
        entry.setRoleName("测试角色");
        entry.setMaxLevel(80);
        entry.setEquipScore(687028L);
        entry.setTotalScore(35100L);
        OfficialQueryData.TrialRank ranking = new OfficialQueryData.TrialRank();
        ranking.setZone("电信区");
        ranking.setServer("乾坤一掷");
        ranking.setName("花间游");
        ranking.setData(List.of(entry));
        ranking.setTime(1784073600L);
        return ranking;
    }

    private GroupAtMessageCreateDto message() {
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setId("message-1");
        message.setGroupOpenid("group-1");
        return message;
    }
}
