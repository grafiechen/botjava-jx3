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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MineCartActionTest {

    @Test
    void shouldFlattenReadableRecordsAndKeepGlobalRequestContract() {
        ApiProperties properties = new ApiProperties();
        properties.setDefaultServer("梦江南");
        Jx3RequestUtil requestUtil = mock(Jx3RequestUtil.class);
        RequestResult requestResult = new RequestResult();
        BaseResult<Object> baseResult = new BaseResult<>();
        baseResult.setCode(200);
        baseResult.setData(List.of(group()));
        when(requestUtil.doPostRequest(anyString(), any())).thenReturn(requestResult);
        when(requestUtil.getResultRealData(requestResult, REGEX.MineCart.getMethodEnum()))
                .thenReturn(baseResult);
        MineCartAction action = new MineCartAction(
                properties, requestUtil, mock(GroupConfigurationService.class));

        BotResponse response = action.doRequest(message(), "关隘首领", REGEX.MineCart);

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(requestUtil).doPostRequest(
                eq(REGEX.MineCart.getMethodEnum().getMethodPath()), params.capture());
        assertEquals(Map.of(), params.getValue());
        assertEquals(BotResponse.ResponseType.IMAGE, response.getResponseType());
        assertEquals("关隘首领", response.getTemplateName());
        Map<?, ?> templateData = (Map<?, ?>) response.getTemplateData();
        assertEquals("全服", templateData.get("server"));
        List<?> views = (List<?>) templateData.get("data");
        MineCartAction.MineCartView view = (MineCartAction.MineCartView) views.get(0);
        assertEquals("剑胆琴心", view.server());
        assertEquals("恶人谷", view.campName());
        assertEquals("保护期", view.statusText());
    }

    private OfficialQueryData.MineCart group() {
        OfficialQueryData.MineCartRecord record = new OfficialQueryData.MineCartRecord();
        record.setZone("电信区");
        record.setLeader("欧薏米·天鹅坪");
        record.setCampName("恶人谷");
        record.setCastle("赤焰关");
        record.setStatusText("保护期");
        OfficialQueryData.MineCart group = new OfficialQueryData.MineCart();
        group.setServer("剑胆琴心");
        group.setData(List.of(record));
        return group;
    }

    private GroupAtMessageCreateDto message() {
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setId("message-1");
        message.setGroupOpenid("group-1");
        return message;
    }
}
