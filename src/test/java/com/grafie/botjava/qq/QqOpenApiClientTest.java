package com.grafie.botjava.qq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.config.TxBotProperty;
import com.grafie.botjava.entity.dto.token.AccessTokenDto;
import com.grafie.botjava.util.RemoteHttpException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QqOpenApiClientTest {

    @Test
    void shouldRefreshTokenAndRetryOnceAfterUnauthorizedResponse() {
        StubQqOpenApiClient client = new StubQqOpenApiClient();

        String result = client.post("/v2/groups/group-1/messages", Map.of("content", "test"), String.class);

        assertEquals("ok", result);
        assertEquals(2, client.tokenRequests);
        assertEquals(2, client.openApiRequests);
    }

    @Test
    void shouldNotRetryRateLimitToAvoidDuplicateRequests() {
        StubQqOpenApiClient client = new StubQqOpenApiClient();
        client.rateLimited = true;

        QqOpenApiException failure = assertThrows(QqOpenApiException.class,
                () -> client.post("/v2/groups/group-1/messages", Map.of(), String.class));

        assertEquals(QqOpenApiException.Category.RATE_LIMIT, failure.getCategory());
        assertEquals(1, client.openApiRequests);
    }

    @Test
    void shouldRejectInvalidPathAndNullPostBodyBeforeRequest() {
        StubQqOpenApiClient client = new StubQqOpenApiClient();

        assertThrows(IllegalArgumentException.class,
                () -> client.post("v2/groups/group-1/messages", Map.of(), String.class));
        assertThrows(IllegalArgumentException.class,
                () -> client.post("/v2/groups/group-1/messages", null, String.class));
        assertThrows(IllegalArgumentException.class,
                () -> client.put("/v2/groups/group-1/messages", null, String.class));
        assertThrows(IllegalArgumentException.class,
                () -> client.get("/v2/groups/group-1", Map.of(), null));
        assertEquals(0, client.tokenRequests);
        assertEquals(0, client.openApiRequests);
    }

    @Test
    void shouldExposeCachedAuthorizationValueForWebSocketIdentify() {
        StubQqOpenApiClient client = new StubQqOpenApiClient();

        assertEquals("QQBot token-1", client.getAuthorizationValue());
        assertEquals("QQBot token-1", client.getAuthorizationValue());
        assertEquals(1, client.tokenRequests);
        assertEquals(0, client.openApiRequests);
    }
    @Test
    void shouldSynchronizeTokenRefreshAcrossConcurrentAuthorizationRequests() throws Exception {
        StubQqOpenApiClient client = new StubQqOpenApiClient();
        client.slowTokenResponse = true;
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            Callable<String> call = client::getAuthorizationValue;
            List<Future<String>> results = executor.invokeAll(List.of(call, call, call, call, call, call, call, call));
            for (Future<String> result : results) {
                assertEquals("QQBot token-1", result.get());
            }
            assertEquals(1, client.tokenRequests);
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
        }
    }

    @Test
    void shouldRefreshAuthorizationWhenTokenWillExpireWithinSixtySeconds() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-12T00:00:00Z"));
        StubQqOpenApiClient client = new StubQqOpenApiClient(clock);
        client.tokenExpiresIn = 120;

        assertEquals("QQBot token-1", client.getAuthorizationValue());
        clock.advance(Duration.ofSeconds(59));
        assertEquals("QQBot token-1", client.getAuthorizationValue());
        clock.advance(Duration.ofSeconds(1));
        assertEquals("QQBot token-2", client.getAuthorizationValue());
        assertEquals(2, client.tokenRequests);
    }

    @Test
    void shouldRouteStandardHttpMethodsThroughSharedAuthentication() {
        StubQqOpenApiClient client = new StubQqOpenApiClient();
        client.unauthorizedOnce = false;

        assertEquals("ok", client.get("/resource", Map.of("limit", 10), String.class));
        assertEquals("ok", client.post("/resource", Map.of("name", "test"), String.class));
        assertEquals("ok", client.put("/resource/1", Map.of("name", "updated"), String.class));
        assertEquals("ok", client.delete("/resource/1", Map.of("reason", "cleanup"), String.class));

        assertEquals(List.of(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE),
                client.methods);
        assertEquals(Map.of("limit", 10), client.queries.get(0));
        assertEquals(Map.of("name", "test"), client.bodies.get(1));
        assertEquals(Map.of("name", "updated"), client.bodies.get(2));
        assertEquals(Map.of("reason", "cleanup"), client.queries.get(3));
        assertEquals(1, client.tokenRequests);
        assertEquals(4, client.openApiRequests);
    }

    private static class StubQqOpenApiClient extends QqOpenApiClient {
        private int tokenRequests;
        private int openApiRequests;
        private boolean rateLimited;
        private boolean unauthorizedOnce = true;
        private int tokenExpiresIn = 300;
        private boolean slowTokenResponse;
        private final List<HttpMethod> methods = new ArrayList<>();
        private final List<Map<String, Object>> queries = new ArrayList<>();
        private final List<Map<String, Object>> bodies = new ArrayList<>();

        private StubQqOpenApiClient() {
            this(Clock.fixed(Instant.parse("2026-08-12T00:00:00Z"), ZoneId.of("UTC")));
        }

        private StubQqOpenApiClient(Clock clock) {
            super(properties(), new ObjectMapper(), clock);
        }

        @Override
        protected AccessTokenDto requestAccessToken(Map<String, Object> request, Map<String, String> headers) {
            tokenRequests++;
            if (slowTokenResponse) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
            }
            AccessTokenDto token = new AccessTokenDto();
            token.setAccessToken("token-" + tokenRequests);
            token.setExpiresIn(tokenExpiresIn);
            return token;
        }

        @Override
        protected <T> T requestOpenApi(HttpMethod method, String path, Map<String, Object> query,
                                       Map<String, Object> request, Map<String, String> headers,
                                       Class<T> responseType) {
            openApiRequests++;
            methods.add(method);
            queries.add(query);
            bodies.add(request);
            if (rateLimited) {
                throw new RemoteHttpException(429, "{\"code\":11281}", null);
            }
            if (unauthorizedOnce && openApiRequests == 1) {
                throw new RemoteHttpException(401, "{\"code\":11245}", null);
            }
            return responseType.cast("ok");
        }
    }

    private static class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
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

    private static TxBotProperty properties() {
        TxBotProperty properties = new TxBotProperty();
        properties.setAccessTokenUrl("https://api.bot.qq.com/app/getAppAccessToken");
        properties.setOpenapiUrl("https://api.bot.qq.com");
        properties.setAppId("app-id");
        properties.setAppSecret("app-secret");
        return properties;
    }
}
