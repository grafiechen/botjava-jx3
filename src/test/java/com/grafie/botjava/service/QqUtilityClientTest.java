package com.grafie.botjava.service;

import com.grafie.botjava.entity.dto.qq.QqInteractionResponseDto;
import com.grafie.botjava.entity.dto.qq.QqUrlLinkRequestDto;
import com.grafie.botjava.entity.dto.qq.QqUrlLinkResultDto;
import com.grafie.botjava.qq.QqOpenApiClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QqUtilityClientTest {

    @Test
    void shouldGenerateUrlLink() {
        QqOpenApiClient openApiClient = mock(QqOpenApiClient.class);
        QqUrlLinkResultDto result = new QqUrlLinkResultDto();
        result.setUrlLink("https://qun.qq.com/link");
        when(openApiClient.post(eq("/v2/generate_url_link"), anyMap(), eq(QqUrlLinkResultDto.class)))
                .thenReturn(result);
        QqUrlLinkClient client = new QqUrlLinkClient(openApiClient);
        QqUrlLinkRequestDto request = new QqUrlLinkRequestDto();
        request.setCallbackData("source-1");

        assertEquals("https://qun.qq.com/link", client.generate(request).getUrlLink());

        ArgumentCaptor<Map<String, Object>> bodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(openApiClient).post(eq("/v2/generate_url_link"), bodyCaptor.capture(), eq(QqUrlLinkResultDto.class));
        assertEquals("source-1", bodyCaptor.getValue().get("callback_data"));
    }

    @Test
    void shouldRespondInteraction() {
        QqOpenApiClient openApiClient = mock(QqOpenApiClient.class);
        QqInteractionClient client = new QqInteractionClient(openApiClient);

        client.respond(" interaction-1 ", QqInteractionResponseDto.of(QqInteractionResponseDto.ADMIN_ONLY));

        ArgumentCaptor<Map<String, Object>> bodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(openApiClient).put(eq("/interactions/interaction-1"), bodyCaptor.capture(), eq(String.class));
        assertEquals(5, bodyCaptor.getValue().get("code"));
    }
}