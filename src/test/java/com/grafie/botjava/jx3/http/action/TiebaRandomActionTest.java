package com.grafie.botjava.jx3.http.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.data.tieba.TiebaRandomData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TiebaRandomActionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldBuildShortReadableTextWithoutPostIdentifiers() throws Exception {
        TiebaRandomData post = objectMapper.readValue("""
                {"id":998877,"tags":"818","zone":"电信区","server":"乾坤一掷",
                 "name":"江湖旧闻","title":"今日茶馆见闻","url":778899,"date":"2026-07-16"}
                """, TiebaRandomData.class);
        Jx3RequestUtil requestUtil = mock(Jx3RequestUtil.class);
        RequestResult requestResult = new RequestResult();
        BaseResult<Object> baseResult = new BaseResult<>();
        baseResult.setCode(200);
        baseResult.setData(List.of(post));
        when(requestUtil.doPostRequest(anyString(), any())).thenReturn(requestResult);
        when(requestUtil.getResultRealData(requestResult, REGEX.TiebaRandom.getMethodEnum())).thenReturn(baseResult);
        TiebaRandomAction action = new TiebaRandomAction(
                new ApiProperties(), requestUtil, mock(GroupConfigurationService.class));

        BotResponse response = action.doRequest(message(), "八卦 818 乾坤一掷", REGEX.TiebaRandom);

        assertEquals(BotResponse.ResponseType.TEXT, response.getResponseType());
        assertTrue(response.getContent().contains("分类：818"));
        assertTrue(response.getContent().contains("区服：电信区 / 乾坤一掷"));
        assertTrue(response.getContent().contains("标题：今日茶馆见闻"));
        assertFalse(response.getContent().contains("998877"));
        assertFalse(response.getContent().contains("778899"));
        assertFalse(response.getContent().contains("url"));
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(requestUtil).doPostRequest(anyString(), captor.capture());
        assertEquals(Map.of("tags", "818", "server", "乾坤一掷", "limit", 1), captor.getValue());
    }

    @Test
    void shouldKeepLegacyClassFieldAsTagsAlias() throws Exception {
        TiebaRandomData post = objectMapper.readValue("{\"class\":\"树洞\"}", TiebaRandomData.class);

        assertEquals("树洞", post.getTags());
    }

    private GroupAtMessageCreateDto message() {
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setId("message-1");
        message.setGroupOpenid("group-1");
        return message;
    }
}
