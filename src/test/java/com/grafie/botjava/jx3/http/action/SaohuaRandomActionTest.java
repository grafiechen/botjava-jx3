package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.data.saohua.SaohuaRandomData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SaohuaRandomActionTest {

    @Test
    void shouldReturnTextOnly() {
        Jx3RequestUtil requestUtil = mock(Jx3RequestUtil.class);
        RequestResult requestResult = new RequestResult();
        SaohuaRandomData data = new SaohuaRandomData();
        data.setText("世界骚如狗，密聊冷如僧，组又拒，密又复。");
        BaseResult<SaohuaRandomData> baseResult = new BaseResult<>();
        baseResult.setCode(200);
        baseResult.setData(data);
        when(requestUtil.doPostRequest(eq(REGEX.SaohuaRandom.getMethodEnum().getMethodPath()), anyMap())).thenReturn(requestResult);
        doReturn(baseResult).when(requestUtil).getResultRealData(requestResult, REGEX.SaohuaRandom.getMethodEnum());

        SaohuaRandomAction action = new SaohuaRandomAction(
                new ApiProperties(), requestUtil, mock(GroupConfigurationService.class));

        BotResponse response = action.doRequest(message(), "骚话", REGEX.SaohuaRandom);

        assertEquals("世界骚如狗，密聊冷如僧，组又拒，密又复。", response.getContent());
    }

    private GroupAtMessageCreateDto message() {
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setGroupOpenid("group-1");
        return message;
    }
}
