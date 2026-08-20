package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.SoundProperties;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.data.sound.SoundConverterData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SoundConverterActionTest {

    @Test
    void shouldNotCallExternalApiWhenSoundIsDisabled() {
        Jx3RequestUtil requestUtil = mock(Jx3RequestUtil.class);
        SoundConverterAction action = new SoundConverterAction(
                new ApiProperties(), requestUtil, mock(GroupConfigurationService.class), new SoundProperties());

        BotResponse response = action.doRequest(message(), "语音 测试", REGEX.SoundConverter);

        assertEquals("语音功能尚未配置。", response.getContent());
        verify(requestUtil, never()).doPostRequest(anyString(), any());
    }

    @Test
    void shouldBuildSoundRequestAndReturnAudioUrl() {
        SoundProperties properties = enabledProperties();
        Jx3RequestUtil requestUtil = mock(Jx3RequestUtil.class);
        RequestResult requestResult = new RequestResult();
        BaseResult<Object> baseResult = new BaseResult<>();
        baseResult.setCode(200);
        SoundConverterData data = new SoundConverterData();
        data.setUrl("https://audio.example.com/result.mp3");
        baseResult.setData(data);
        when(requestUtil.doPostRequest(anyString(), any())).thenReturn(requestResult);
        when(requestUtil.getResultRealData(requestResult, REGEX.SoundConverter.getMethodEnum()))
                .thenReturn(baseResult);
        SoundConverterAction action = new SoundConverterAction(
                new ApiProperties(), requestUtil, mock(GroupConfigurationService.class), properties);

        BotResponse response = action.doRequest(message(), "语音 剑网三真好玩", REGEX.SoundConverter);

        assertEquals(BotResponse.ResponseType.AUDIO_URL, response.getResponseType());
        assertEquals("https://audio.example.com/result.mp3", response.getAudioUrl());
        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(requestUtil).doPostRequest(eq(REGEX.SoundConverter.getMethodEnum().getMethodPath()), params.capture());
        assertEquals("app-key", params.getValue().get("appkey"));
        assertEquals("access-key", params.getValue().get("access"));
        assertEquals("secret-key", params.getValue().get("secret"));
        assertEquals("剑网三真好玩", params.getValue().get("text"));
        assertEquals(16000, params.getValue().get("sample_rate"));
    }

    private static SoundProperties enabledProperties() {
        SoundProperties properties = new SoundProperties();
        properties.setEnabled(true);
        properties.setAppkey("app-key");
        properties.setAccess("access-key");
        properties.setSecret("secret-key");
        return properties;
    }

    private static GroupAtMessageCreateDto message() {
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setId("message-1");
        message.setGroupOpenid("group-1");
        return message;
    }
}
