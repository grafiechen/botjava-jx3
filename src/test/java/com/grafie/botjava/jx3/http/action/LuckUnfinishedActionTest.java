package com.grafie.botjava.jx3.http.action;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.data.luck.unfinished.Last;
import com.grafie.botjava.jx3.http.data.luck.unfinished.LuckUnfinishedData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LuckUnfinishedActionTest {

    @Test
    void shouldDeserializeOfficialTypeAndLegacyLast() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, LuckUnfinishedData.class);

        List<LuckUnfinishedData> current = mapper.readValue("""
                [{"name":"三山四海","type":"绝世奇遇","level":2}]
                """, type);
        List<LuckUnfinishedData> legacy = mapper.readValue("""
                [{"name":"旧奇遇","level":1,"last":{"id":99,"server":"旧服","name":"旧角色","time":1764574651}}]
                """, type);

        assertEquals("三山四海", current.get(0).getName());
        assertEquals("绝世奇遇", current.get(0).getType());
        assertNull(current.get(0).getLast());
        assertEquals("旧服", legacy.get(0).getLast().getServer());
        assertEquals("2025-12-01 15:37:31", legacy.get(0).getLast().getTime());
    }

    @Test
    void shouldBuildUnfinishedImageWithoutLegacyLast() {
        Last last = new Last();
        last.setId(99L);
        last.setServer("旧服");
        LuckUnfinishedData unfinished = new LuckUnfinishedData();
        unfinished.setName("三山四海");
        unfinished.setType("绝世奇遇");
        unfinished.setLevel(2);
        unfinished.setLast(last);

        Jx3RequestUtil requestUtil = mock(Jx3RequestUtil.class);
        RequestResult requestResult = new RequestResult();
        BaseResult<Object> baseResult = new BaseResult<>();
        baseResult.setCode(200);
        baseResult.setData(List.of(unfinished));
        when(requestUtil.doPostRequest(anyString(), any())).thenReturn(requestResult);
        when(requestUtil.getResultRealData(requestResult, REGEX.LuckUnfinished.getMethodEnum()))
                .thenReturn(baseResult);
        ApiProperties properties = new ApiProperties();
        properties.setDefaultServer("梦江南");
        LuckUnfinishedAction action = new LuckUnfinishedAction(
                properties, requestUtil, mock(GroupConfigurationService.class));

        BotResponse response = action.doRequest(message(), "未做奇遇 乾坤一掷 加菲", REGEX.LuckUnfinished);

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(requestUtil).doPostRequest(
                eq(REGEX.LuckUnfinished.getMethodEnum().getMethodPath()), params.capture());
        assertEquals(Map.of("server", "乾坤一掷", "name", "加菲"), params.getValue());
        assertEquals(BotResponse.ResponseType.IMAGE, response.getResponseType());
        assertEquals("未做奇遇", response.getTemplateName());
        Map<?, ?> templateData = (Map<?, ?>) response.getTemplateData();
        assertEquals("乾坤一掷", templateData.get("server"));
        assertEquals("加菲", templateData.get("name"));
        LuckUnfinishedAction.UnfinishedView view =
                (LuckUnfinishedAction.UnfinishedView) ((List<?>) templateData.get("data")).get(0);
        assertEquals("三山四海", view.name());
        assertEquals("绝世奇遇", view.type());
        assertFalse(Arrays.stream(view.getClass().getRecordComponents())
                .anyMatch(component -> component.getName().equals("last")));
    }

    private GroupAtMessageCreateDto message() {
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setId("message-1");
        message.setGroupOpenid("group-1");
        return message;
    }
}
