package com.grafie.botjava.jx3.http.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.MethodEnum;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.cache.Jx3ApiResponseCache;
import com.grafie.botjava.observability.BotMetrics;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class Jx3RequestUtilMetricsTest {

    @Test
    void shouldRecordCacheHitWithoutCallingRemoteEndpoint() {
        ApiProperties properties = new ApiProperties();
        properties.setApiUrl("https://example.invalid");
        properties.setApiToken("test-token");
        Jx3ApiResponseCache cache = mock(Jx3ApiResponseCache.class);
        BotMetrics metrics = mock(BotMetrics.class);
        RequestResult cached = new RequestResult();
        cached.setCode(200);
        Map<String, Object> params = Map.of("server", "乾坤一掷");
        org.mockito.Mockito.when(cache.get(eq("/data/status/check"), anyMap()))
                .thenReturn(Optional.of(cached));
        org.mockito.Mockito.when(cache.isCacheable("/data/status/check")).thenReturn(true);
        Jx3RequestUtil requestUtil = new Jx3RequestUtil(
                properties, new ObjectMapper(), cache, metrics);

        RequestResult result = requestUtil.doPostRequest("/data/status/check", params);

        assertSame(cached, result);
        verify(metrics).recordJx3Request(
                eq("/data/status/check"), eq("cache"), eq("success"), anyLong());
        verify(metrics).recordJx3Cache("/data/status/check", "hit");
    }
    @Test
    void shouldUseV2TokenForLevelTwoEndpoint() {
        ApiProperties properties = new ApiProperties();
        properties.setApiUrl("https://example.invalid");
        properties.setApiToken("level-one-token");
        properties.setApiV2Token("level-two-token");
        Jx3ApiResponseCache cache = mock(Jx3ApiResponseCache.class);
        BotMetrics metrics = mock(BotMetrics.class);
        RequestResult cached = new RequestResult();
        cached.setCode(200);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        org.mockito.Mockito.when(cache.get(eq(MethodEnum.DATA_AUCTION_RECORDS.getMethodPath()), paramsCaptor.capture()))
                .thenReturn(Optional.of(cached));
        Jx3RequestUtil requestUtil = new Jx3RequestUtil(
                properties, new ObjectMapper(), cache, metrics);

        RequestResult result = requestUtil.doPostRequest(MethodEnum.DATA_AUCTION_RECORDS, Map.of("server", "乾坤一掷"));

        assertSame(cached, result);
        assertEquals("level-two-token", paramsCaptor.getValue().get("token"));
    }

    @Test
    void shouldFallbackToApiTokenWhenV2TokenIsBlank() {
        ApiProperties properties = new ApiProperties();
        properties.setApiUrl("https://example.invalid");
        properties.setApiToken("single-token");
        Jx3ApiResponseCache cache = mock(Jx3ApiResponseCache.class);
        BotMetrics metrics = mock(BotMetrics.class);
        RequestResult cached = new RequestResult();
        cached.setCode(200);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        org.mockito.Mockito.when(cache.get(eq(MethodEnum.DATA_AUCTION_RECORDS.getMethodPath()), paramsCaptor.capture()))
                .thenReturn(Optional.of(cached));
        Jx3RequestUtil requestUtil = new Jx3RequestUtil(
                properties, new ObjectMapper(), cache, metrics);

        RequestResult result = requestUtil.doPostRequest(MethodEnum.DATA_AUCTION_RECORDS, Map.of("server", "乾坤一掷"));

        assertSame(cached, result);
        assertEquals("single-token", paramsCaptor.getValue().get("token"));
    }
}
