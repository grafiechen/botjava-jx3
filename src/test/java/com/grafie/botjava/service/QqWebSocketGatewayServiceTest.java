package com.grafie.botjava.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.config.QqIngressProperties;
import com.grafie.botjava.config.QqWebSocketProperties;
import com.grafie.botjava.contants.OpCode;
import com.grafie.botjava.qq.QqOpenApiClient;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QqWebSocketGatewayServiceTest {

    @Test
    void shouldBuildIdentifyFrameFromConfiguredIntentsShardAndToken() {
        QqWebSocketProperties websocket = new QqWebSocketProperties();
        websocket.setIntents(12345);
        websocket.setShardId(1);
        websocket.setShardCount(4);
        QqOpenApiClient openApiClient = mock(QqOpenApiClient.class);
        when(openApiClient.getAuthorizationValue()).thenReturn("QQBot token-1");
        QqWebSocketGatewayService service = service(websocket, openApiClient);

        Map<String, Object> frame = service.buildIdentifyFrame();

        assertEquals(OpCode.IDENTIFY.getCode(), frame.get("op"));
        Map<?, ?> data = (Map<?, ?>) frame.get("d");
        assertEquals("QQBot token-1", data.get("token"));
        assertEquals(12345, data.get("intents"));
        assertEquals(List.of(1, 4), data.get("shard"));
    }

    @Test
    void shouldAllowNullSequenceHeartbeatFrame() {
        QqWebSocketGatewayService service = service(new QqWebSocketProperties(), mock(QqOpenApiClient.class));

        Map<String, Object> frame = service.buildHeartbeatFrame();

        assertEquals(OpCode.HEARTBEAT.getCode(), frame.get("op"));
        assertNull(frame.get("d"));
    }

    @Test
    void staleGenerationMustNotScheduleReconnectOrReplaceCurrentConnection() {
        QqWebSocketGatewayService service = service(new QqWebSocketProperties(), mock(QqOpenApiClient.class));
        try {
            AtomicBoolean running = (AtomicBoolean) ReflectionTestUtils.getField(service, "running");
            AtomicLong generation = (AtomicLong) ReflectionTestUtils.getField(service, "connectionGeneration");
            running.set(true);
            generation.set(8L);

            ReflectionTestUtils.invokeMethod(service, "scheduleReconnect", 7L, Duration.ofDays(1));
            assertEquals(8L, generation.get());
            assertNull(ReflectionTestUtils.getField(service, "reconnectTask"));

            ReflectionTestUtils.invokeMethod(service, "scheduleReconnect", 8L, Duration.ofDays(1));
            assertEquals(9L, generation.get());
            assertNotNull(ReflectionTestUtils.getField(service, "reconnectTask"));

            ReflectionTestUtils.invokeMethod(service, "scheduleReconnect", 8L, Duration.ZERO);
            assertEquals(9L, generation.get());
        } finally {
            service.shutdown();
        }
    }
    private static QqWebSocketGatewayService service(QqWebSocketProperties websocket,
                                                     QqOpenApiClient openApiClient) {
        return new QqWebSocketGatewayService(
                new QqIngressProperties(),
                websocket,
                mock(QqGatewayClient.class),
                openApiClient,
                mock(BotMessageService.class),
                new ObjectMapper()
        );
    }
}
