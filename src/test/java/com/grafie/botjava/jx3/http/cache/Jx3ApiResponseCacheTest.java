package com.grafie.botjava.jx3.http.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.entity.Jx3ApiCacheEntry;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.mapper.Jx3ApiCacheEntryMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Jx3ApiResponseCacheTest {

    @Test
    void shouldSerializeSuccessfulQueryWithoutUsingTokenInKey() {
        MutableClock clock = new MutableClock();
        Jx3ApiCacheEntryMapper mapper = mock(Jx3ApiCacheEntryMapper.class);
        Jx3ApiResponseCache cache = new Jx3ApiResponseCache(mapper, new ObjectMapper(), clock);

        cache.put("/data/status/check",
                Map.of("server", "乾坤一掷", "token", "token-1"), success());

        ArgumentCaptor<Jx3ApiCacheEntry> entryCaptor = ArgumentCaptor.forClass(Jx3ApiCacheEntry.class);
        verify(mapper).saveAndFlush(entryCaptor.capture());
        Jx3ApiCacheEntry entry = entryCaptor.getValue();
        when(mapper.findByCacheKey(entry.getCacheKey())).thenReturn(entry);
        RequestResult cached = cache.get("/data/status/check",
                Map.of("server", "乾坤一掷", "token", "token-2")).orElseThrow();

        assertEquals(200, cached.getCode());
        assertEquals("success", cached.getMsg());
        assertFalse(entry.getResponseJson().contains("token-1"));
    }

    @Test
    void shouldDeleteEntryAtTtlBoundary() {
        MutableClock clock = new MutableClock();
        Jx3ApiCacheEntryMapper mapper = mock(Jx3ApiCacheEntryMapper.class);
        Jx3ApiResponseCache cache = new Jx3ApiResponseCache(mapper, new ObjectMapper(), clock);
        Jx3ApiCacheEntry entry = entry(clock.instant().plusSeconds(30), "{\"code\":200}");
        when(mapper.findByCacheKey(anyString())).thenReturn(entry);

        clock.advance(Duration.ofSeconds(30));

        assertFalse(cache.get("/data/status/check", Map.of("server", "乾坤一掷")).isPresent());
        verify(mapper).delete(entry);
    }

    @Test
    void shouldDeleteCorruptedJsonAndTreatItAsCacheMiss() {
        MutableClock clock = new MutableClock();
        Jx3ApiCacheEntryMapper mapper = mock(Jx3ApiCacheEntryMapper.class);
        Jx3ApiResponseCache cache = new Jx3ApiResponseCache(mapper, new ObjectMapper(), clock);
        Jx3ApiCacheEntry entry = entry(clock.instant().plusSeconds(30), "not-json");
        when(mapper.findByCacheKey(anyString())).thenReturn(entry);

        assertFalse(cache.get("/data/status/check", Map.of()).isPresent());
        verify(mapper).delete(entry);
    }

    @Test
    void shouldNotCacheRealtimeOrFailedQuery() {
        Jx3ApiCacheEntryMapper mapper = mock(Jx3ApiCacheEntryMapper.class);
        Jx3ApiResponseCache cache = new Jx3ApiResponseCache(
                mapper, new ObjectMapper(), Clock.systemUTC());
        RequestResult failed = new RequestResult();
        failed.setCode(500);

        cache.put("/data/role/attribute", Map.of("name", "角色名"), success());
        cache.put("/data/status/check", Map.of("server", "乾坤一掷"), failed);

        verify(mapper, never()).updateEntry(any(), any(), any(), any(), any());
        verify(mapper, never()).saveAndFlush(any());
        assertTrue(cache.isCacheable("/data/status/check"));
        assertFalse(cache.isCacheable("/data/role/attribute"));
    }

    @Test
    void shouldCleanupExpiredEntries() {
        Jx3ApiCacheEntryMapper mapper = mock(Jx3ApiCacheEntryMapper.class);
        MutableClock clock = new MutableClock();
        Jx3ApiResponseCache cache = new Jx3ApiResponseCache(mapper, new ObjectMapper(), clock);

        cache.cleanupExpired();

        verify(mapper).deleteByExpiresAtLessThanEqual(clock.instant());
    }

    @Test
    void shouldSkipResponseLargerThanOneMegabyte() {
        Jx3ApiCacheEntryMapper mapper = mock(Jx3ApiCacheEntryMapper.class);
        Jx3ApiResponseCache cache = new Jx3ApiResponseCache(
                mapper, new ObjectMapper(), Clock.systemUTC());
        RequestResult large = success();
        large.setData("x".repeat(1_000_001));

        cache.put("/data/status/check", Map.of(), large);

        verify(mapper, never()).updateEntry(any(), any(), any(), any(), any());
        verify(mapper, never()).saveAndFlush(any());
    }

    @Test
    void shouldTreatCacheDatabaseFailureAsMissAndIgnoreWriteFailure() {
        Jx3ApiCacheEntryMapper mapper = mock(Jx3ApiCacheEntryMapper.class);
        Jx3ApiResponseCache cache = new Jx3ApiResponseCache(
                mapper, new ObjectMapper(), Clock.systemUTC());
        when(mapper.findByCacheKey(anyString())).thenThrow(new IllegalStateException("database unavailable"));
        when(mapper.updateEntry(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("database unavailable"));

        assertFalse(cache.get("/data/status/check", Map.of()).isPresent());
        cache.put("/data/status/check", Map.of(), success());
    }

    private static Jx3ApiCacheEntry entry(Instant expiresAt, String responseJson) {
        Jx3ApiCacheEntry entry = new Jx3ApiCacheEntry();
        entry.setCacheKey("cache-key");
        entry.setRequestPath("/data/status/check");
        entry.setResponseJson(responseJson);
        entry.setExpiresAt(expiresAt);
        return entry;
    }

    private static RequestResult success() {
        RequestResult result = new RequestResult();
        result.setCode(200);
        result.setMsg("success");
        result.setData(Map.of("status", "开服"));
        return result;
    }

    private static class MutableClock extends Clock {
        private Instant instant = Instant.parse("2026-07-13T00:00:00Z");

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
