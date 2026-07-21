package com.grafie.botjava.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.entity.Jx3ApiCacheEntry;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.cache.Jx3ApiResponseCache;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
        "tx.bot.openapi-url=https://example.test",
        "tx.bot.access-token-url=https://example.test/token",
        "tx.bot.app-id=test-app",
        "tx.bot.app-secret=test-secret",
        "jx3api.api.api-token=test-token",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@ContextConfiguration(classes = Jx3ApiCacheEntryMapperTest.TestApplication.class)
class Jx3ApiCacheEntryMapperTest {

    @Autowired
    private Jx3ApiCacheEntryMapper cacheMapper;

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = Jx3ApiCacheEntry.class)
    @EnableJpaRepositories(basePackageClasses = Jx3ApiCacheEntryMapper.class)
    static class TestApplication {
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldShareSerializedCacheAcrossServiceInstancesAndUpdateExistingEntry() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-13T00:00:00Z"), ZoneOffset.UTC);
        Jx3ApiResponseCache first = new Jx3ApiResponseCache(cacheMapper, new ObjectMapper(), clock);
        Jx3ApiResponseCache second = new Jx3ApiResponseCache(cacheMapper, new ObjectMapper(), clock);
        Map<String, Object> firstParams = Map.of("server", "乾坤一掷", "token", "token-1");
        Map<String, Object> secondParams = Map.of("server", "乾坤一掷", "token", "token-2");

        first.put("/data/status/check", firstParams, result("首次"));
        assertEquals("首次", second.get("/data/status/check", secondParams)
                .orElseThrow().getMsg());

        second.put("/data/status/check", secondParams, result("更新"));
        assertEquals("更新", first.get("/data/status/check", firstParams)
                .orElseThrow().getMsg());
        assertEquals(1, cacheMapper.count());
    }

    private static RequestResult result(String message) {
        RequestResult result = new RequestResult();
        result.setCode(200);
        result.setMsg(message);
        result.setData(Map.of("server", "乾坤一掷", "status", "开服"));
        return result;
    }
}
