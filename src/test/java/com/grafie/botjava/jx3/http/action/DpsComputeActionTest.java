package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.data.other.DpsComputeData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DpsComputeActionTest {

    @Test
    void shouldUseConfiguredDpsEndpointTokenAndModel() {
        ApiProperties properties = new ApiProperties();
        properties.setName("test-bot");
        properties.setTicket("ticket-value");
        properties.setDpsToken("dps-token-value");
        properties.setDpsServiceUrl("https://dps.example.com");
        properties.setDpsServicePath("/compute");
        properties.setDpsModel("无界");
        Jx3RequestUtil requestUtil = mock(Jx3RequestUtil.class);
        GroupConfigurationService groupConfigurationService = mock(GroupConfigurationService.class);
        when(groupConfigurationService.findServer("group-1")).thenReturn(java.util.Optional.of("乾坤一掷"));
        RequestResult roleAttributeResult = new RequestResult();
        roleAttributeResult.setData(new DpsComputeData());
        when(requestUtil.doPostRequest(eq(REGEX.RoleAttribute.getMethodEnum().getMethodPath()), anyMap()))
                .thenReturn(roleAttributeResult);
        CapturingDpsComputeAction action = new CapturingDpsComputeAction(
                properties, requestUtil, groupConfigurationService);
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setGroupOpenid("group-1");

        BotResponse response = action.doRequest(message, "DPS 乾坤一掷 加菲 循环A",
                REGEX.DpsCompute);

        assertEquals(BotResponse.ResponseType.IMAGE_URL, response.getResponseType());
        assertEquals("https://image.example.com/dps.png", response.getImageUrl());
        assertEquals("https://dps.example.com", action.serviceUrl);
        assertEquals("/compute", action.servicePath);
        assertEquals("dps-token-value", action.headers.get("token"));
        assertEquals("无界", action.requestBody.get("model"));
        assertEquals("test-bot", action.requestBody.get("bot"));
        assertEquals("循环A", action.requestBody.get("loop"));
        ArgumentCaptor<Map<String, Object>> roleRequest = ArgumentCaptor.forClass(Map.class);
        verify(requestUtil).doPostRequest(eq(REGEX.RoleAttribute.getMethodEnum().getMethodPath()),
                roleRequest.capture());
        assertEquals("乾坤一掷", roleRequest.getValue().get("server"));
        assertEquals("加菲", roleRequest.getValue().get("name"));
        assertEquals("ticket-value", roleRequest.getValue().get("ticket"));
    }

    private static class CapturingDpsComputeAction extends DpsComputeAction {
        private final ApiProperties properties;
        private String serviceUrl;
        private String servicePath;
        private Map<String, Object> requestBody;
        private Map<String, String> headers;

        private CapturingDpsComputeAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil,
                                          GroupConfigurationService groupConfigurationService) {
            super(apiProperties, jx3RequestUtil, groupConfigurationService);
            this.properties = apiProperties;
        }

        @Override
        protected Map<String, Object> callDpsService(Map<String, Object> requestBody,
                                                     Map<String, String> headers) {
            this.serviceUrl = properties.getDpsServiceUrl();
            this.servicePath = properties.getDpsServicePath();
            this.requestBody = requestBody;
            this.headers = headers;
            return Map.of("code", 200, "data", Map.of("image", "https://image.example.com/dps.png"));
        }
    }
}
