package com.grafie.botjava.jx3.http.action;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.data.luck.LuckAdventureData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LuckRecentActionTest {

    @Test
    void shouldDeserializeOfficialRecentFields() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, LuckAdventureData.class);

        List<LuckAdventureData> recent = mapper.readValue("""
                [{"id":4940141,"zone":"电信区","server":"梦江南","name":"往矣",
                  "event":"侠者成歌","source":1,"status":1,"time":1764574651}]
                """, type);

        assertEquals(4940141L, recent.get(0).getId());
        assertEquals(1, recent.get(0).getSource());
        assertEquals("侠者成歌", recent.get(0).getEvent());
        assertEquals("2025-12-01 15:37:31", recent.get(0).getTime());
    }

    @Test
    void shouldBuildRecentImageWithoutInternalNumericFields() {
        LuckAdventureData recent = new LuckAdventureData();
        recent.setId(4940141L);
        recent.setZone("电信区");
        recent.setServer("梦江南");
        recent.setName("往矣");
        recent.setEvent("侠者成歌");
        recent.setSource(1);
        recent.setStatus(1);
        recent.setTime(1764574651L);

        Jx3RequestUtil requestUtil = mock(Jx3RequestUtil.class);
        RequestResult requestResult = new RequestResult();
        BaseResult<Object> baseResult = new BaseResult<>();
        baseResult.setCode(200);
        baseResult.setData(List.of(recent));
        when(requestUtil.doPostRequest(anyString(), any())).thenReturn(requestResult);
        when(requestUtil.getResultRealData(requestResult, REGEX.LuckRecent.getMethodEnum()))
                .thenReturn(baseResult);
        ApiProperties properties = new ApiProperties();
        properties.setDefaultServer("乾坤一掷");
        LuckRecentAction action = new LuckRecentAction(
                properties, requestUtil, mock(GroupConfigurationService.class));

        BotResponse response = action.doRequest(message(), "近期奇遇 梦江南", REGEX.LuckRecent);

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(requestUtil).doPostRequest(
                eq(REGEX.LuckRecent.getMethodEnum().getMethodPath()), params.capture());
        assertEquals(Map.of("server", "梦江南"), params.getValue());
        assertEquals(BotResponse.ResponseType.IMAGE, response.getResponseType());
        assertEquals("近期奇遇", response.getTemplateName());
        Map<?, ?> templateData = (Map<?, ?>) response.getTemplateData();
        assertEquals("梦江南", templateData.get("server"));
        LuckRecentAction.RecentView view =
                (LuckRecentAction.RecentView) ((List<?>) templateData.get("data")).get(0);
        assertEquals("往矣", view.name());
        assertEquals("侠者成歌", view.event());
        assertFalse(Arrays.stream(view.getClass().getRecordComponents())
                .anyMatch(component -> component.getName().equals("id")
                        || component.getName().equals("source")
                        || component.getName().equals("status")));
    }

    private GroupAtMessageCreateDto message() {
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setId("message-1");
        message.setGroupOpenid("group-1");
        return message;
    }
}
