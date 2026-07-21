package com.grafie.botjava.jx3.http.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.entity.Jx3ApiCacheEntry;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.mapper.Jx3ApiCacheEntryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 基于数据库的 JX3API 跨实例短时缓存。
 */
@Slf4j
@Component
public class Jx3ApiResponseCache {

    private static final int MAX_RESPONSE_BYTES = 1_000_000;
    private static final Map<String, Duration> CACHE_POLICIES = Map.of(
            "/data/status/check", Duration.ofSeconds(30),
            "/data/active/calendar", Duration.ofMinutes(5),
            "/data/news/announce", Duration.ofMinutes(5),
            "/data/trade/demon", Duration.ofMinutes(2),
            "/data/trade/item/records", Duration.ofMinutes(2),
            "/data/recruit/search", Duration.ofSeconds(30),
            "/data/sand/records", Duration.ofMinutes(2),
            "/data/active/monster", Duration.ofMinutes(5)
    );

    private final Jx3ApiCacheEntryMapper cacheMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public Jx3ApiResponseCache(Jx3ApiCacheEntryMapper cacheMapper, ObjectMapper objectMapper) {
        this(cacheMapper, objectMapper, Clock.systemUTC());
    }

    public Jx3ApiResponseCache(Jx3ApiCacheEntryMapper cacheMapper, ObjectMapper objectMapper, Clock clock) {
        this.cacheMapper = cacheMapper;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public Optional<RequestResult> get(String path, Map<String, Object> params) {
        if (!isCacheable(path)) {
            return Optional.empty();
        }
        String cacheKey = buildKey(path, params);
        try {
            Jx3ApiCacheEntry entry = cacheMapper.findByCacheKey(cacheKey);
            if (entry == null) {
                return Optional.empty();
            }
            if (!clock.instant().isBefore(entry.getExpiresAt())) {
                cacheMapper.delete(entry);
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(entry.getResponseJson(), RequestResult.class));
        } catch (JsonProcessingException e) {
            deleteQuietly(cacheKey);
            log.warn("JX3API 缓存数据损坏，已删除。path=>{}，cacheKey=>{}", path, cacheKey);
            return Optional.empty();
        } catch (RuntimeException e) {
            log.warn("读取 JX3API 缓存失败，退化为远程查询。path=>{}，reason=>{}",
                    path, e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    public void put(String path, Map<String, Object> params, RequestResult result) {
        Duration ttl = CACHE_POLICIES.get(path);
        if (ttl == null || result == null || result.getCode() == null || result.getCode() != 200) {
            return;
        }
        try {
            String responseJson = objectMapper.writeValueAsString(result);
            if (responseJson.getBytes(StandardCharsets.UTF_8).length > MAX_RESPONSE_BYTES) {
                log.warn("JX3API 响应超过缓存上限，跳过写入。path=>{}", path);
                return;
            }
            String cacheKey = buildKey(path, params);
            Instant now = clock.instant();
            Instant expiresAt = now.plus(ttl);
            if (cacheMapper.updateEntry(cacheKey, path, responseJson, expiresAt, now) == 0) {
                insertEntry(cacheKey, path, responseJson, expiresAt);
            }
        } catch (JsonProcessingException e) {
            log.warn("序列化 JX3API 缓存失败，跳过写入。path=>{}", path);
        } catch (RuntimeException e) {
            log.warn("写入 JX3API 缓存失败，不影响远程查询结果。path=>{}，reason=>{}",
                    path, e.getClass().getSimpleName());
        }
    }

    @Scheduled(cron = "${jx3api.cache.cleanup-cron:0 45 3 * * *}")
    public void cleanupExpired() {
        try {
            long deleted = cacheMapper.deleteByExpiresAtLessThanEqual(clock.instant());
            if (deleted > 0) {
                log.info("清理过期 JX3API 缓存，deleted=>{}", deleted);
            }
        } catch (RuntimeException e) {
            log.warn("清理过期 JX3API 缓存失败，reason=>{}", e.getClass().getSimpleName());
        }
    }

    public void clear() {
        try {
            cacheMapper.deleteAllInBatch();
        } catch (RuntimeException e) {
            log.warn("清空 JX3API 缓存失败，reason=>{}", e.getClass().getSimpleName());
        }
    }

    public boolean isCacheable(String path) {
        return CACHE_POLICIES.containsKey(path);
    }

    private void insertEntry(String cacheKey, String path, String responseJson, Instant expiresAt) {
        Jx3ApiCacheEntry entry = new Jx3ApiCacheEntry();
        entry.setCacheKey(cacheKey);
        entry.setRequestPath(path);
        entry.setResponseJson(responseJson);
        entry.setExpiresAt(expiresAt);
        try {
            cacheMapper.saveAndFlush(entry);
        } catch (DataIntegrityViolationException race) {
            Instant now = clock.instant();
            cacheMapper.updateEntry(cacheKey, path, responseJson, expiresAt, now);
        }
    }

    private void deleteQuietly(String cacheKey) {
        try {
            Jx3ApiCacheEntry entry = cacheMapper.findByCacheKey(cacheKey);
            if (entry != null) {
                cacheMapper.delete(entry);
            }
        } catch (RuntimeException ignored) {
            // 损坏缓存清理失败不应影响主请求。
        }
    }

    private String buildKey(String path, Map<String, Object> params) {
        Map<String, Object> safeParams = params == null ? Map.of() : params;
        String normalized = safeParams.entrySet().stream()
                .filter(entry -> !"token".equalsIgnoreCase(entry.getKey()))
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(entry -> entry.getKey() + "=" + String.valueOf(entry.getValue()))
                .collect(Collectors.joining("&"));
        return sha256(path + ":" + normalized);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", e);
        }
    }
}
