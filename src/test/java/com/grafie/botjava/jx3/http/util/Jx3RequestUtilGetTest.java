package com.grafie.botjava.jx3.http.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.MethodEnum;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.cache.Jx3ApiResponseCache;
import com.grafie.botjava.observability.BotMetrics;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Jx3RequestUtilGetTest {

    @Test
    void shouldSendWanbaolouIdAndLevelTwoTokenAsGetQuery() throws Exception {
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> query = new AtomicReference<>();
        AtomicReference<String> tokenHeader = new AtomicReference<>();
        AtomicReference<byte[]> body = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/trade/wanbaolou", exchange -> {
            method.set(exchange.getRequestMethod());
            query.set(exchange.getRequestURI().getRawQuery());
            tokenHeader.set(exchange.getRequestHeaders().getFirst("token"));
            body.set(exchange.getRequestBody().readAllBytes());
            byte[] response = "{\"code\":200,\"msg\":\"success\",\"data\":{\"id\":\"1405435120446099456\"}}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            ApiProperties properties = properties(server.getAddress().getPort());
            Jx3ApiResponseCache cache = mock(Jx3ApiResponseCache.class);
            when(cache.get(anyString(), anyMap())).thenReturn(Optional.empty());
            Jx3RequestUtil requestUtil = new Jx3RequestUtil(
                    properties, new ObjectMapper(), cache, mock(BotMetrics.class));

            RequestResult result = requestUtil.doGetRequest(
                    MethodEnum.DATA_TRADE_WANBAOLOU,
                    Map.of("id", "1405435120446099456"));

            assertEquals(200, result.getCode());
            assertEquals("GET", method.get());
            assertNotNull(query.get());
            assertTrue(query.get().contains("id=1405435120446099456"));
            assertTrue(query.get().contains("token=level-two-token"));
            assertEquals("level-two-token", tokenHeader.get());
            assertNotNull(body.get());
            assertEquals(0, body.get().length);
            assertFalse(query.get().contains("level-one-token"));
        } finally {
            server.stop(0);
        }
    }

    private ApiProperties properties(int port) throws IOException {
        ApiProperties properties = new ApiProperties();
        properties.setApiUrl("http://127.0.0.1:" + port);
        properties.setApiToken("level-one-token");
        properties.setApiV2Token("level-two-token");
        return properties;
    }
}