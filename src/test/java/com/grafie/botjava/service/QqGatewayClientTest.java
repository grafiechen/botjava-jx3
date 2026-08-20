package com.grafie.botjava.service;

import com.grafie.botjava.entity.dto.qq.QqGatewayDto;
import com.grafie.botjava.qq.QqOpenApiClient;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QqGatewayClientTest {

    @Test
    void shouldFetchGatewayAndGatewayBotFromOfficialPathsAndReuseCache() {
        QqOpenApiClient openApiClient = mock(QqOpenApiClient.class);
        QqGatewayDto gateway = new QqGatewayDto();
        gateway.setUrl("wss://api.bot.qq.com/websocket/");
        QqGatewayDto gatewayBot = new QqGatewayDto();
        gatewayBot.setUrl("wss://api.bot.qq.com/websocket/bot");
        gatewayBot.setShards(2);
        when(openApiClient.get(eq("/gateway"), eq(Map.of()), eq(QqGatewayDto.class))).thenReturn(gateway);
        when(openApiClient.get(eq("/gateway/bot"), eq(Map.of()), eq(QqGatewayDto.class))).thenReturn(gatewayBot);
        QqGatewayClient client = new QqGatewayClient(openApiClient);

        assertEquals("wss://api.bot.qq.com/websocket/", client.getGateway().getUrl());
        assertEquals("wss://api.bot.qq.com/websocket/", client.getGateway().getUrl());
        assertEquals(2, client.getGatewayBot().getShards());
        assertEquals(2, client.getGatewayBot().getShards());
        verify(openApiClient, times(1)).get(eq("/gateway"), eq(Map.of()), eq(QqGatewayDto.class));
        verify(openApiClient, times(1)).get(eq("/gateway/bot"), eq(Map.of()), eq(QqGatewayDto.class));
    }
}
