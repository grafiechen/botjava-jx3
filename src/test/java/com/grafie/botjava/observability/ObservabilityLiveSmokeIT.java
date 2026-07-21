package com.grafie.botjava.observability;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Explicitly enabled live acceptance test for the target Prometheus scrape
 * endpoint. It validates exported metric names and label hygiene without
 * printing the endpoint URL or response body.
 */
class ObservabilityLiveSmokeIT {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @Test
    void shouldExposeBotMetricsFromTargetPrometheusEndpoint() throws Exception {
        Assumptions.assumeTrue("true".equalsIgnoreCase(environment("OBSERVABILITY_LIVE_SMOKE")),
                "Set OBSERVABILITY_LIVE_SMOKE=true to query target Prometheus endpoint");
        ObservabilityLiveSmokePlan.Plan plan = ObservabilityLiveSmokePlan.from(System.getenv());

        HttpResponse<String> response = fetch(plan.prometheusUri());
        assertEquals(200, response.statusCode(), "Prometheus endpoint must return HTTP 200");
        String body = response.body();
        assertTrue(body.contains("bot_command_duration_seconds"),
                "Prometheus scrape must include command duration metrics");
        assertTrue(body.contains("bot_jx3_request_duration_seconds"),
                "Prometheus scrape must include JX3 request metrics");
        assertTrue(body.contains("bot_jx3_cache_requests_total"),
                "Prometheus scrape must include JX3 cache metrics");
        assertTrue(body.contains("bot_qq_request_duration_seconds"),
                "Prometheus scrape must include QQ request metrics");
        assertFalse(body.contains("group_openid"));
        assertFalse(body.contains("member_openid"));
        assertFalse(body.contains("message_id"));
        assertFalse(body.contains("role_name"));
        assertFalse(body.contains("content="));

        URI uri = plan.prometheusUri();
        System.out.printf("OBSERVABILITY_PROMETHEUS\tSUCCESS\thost=%s\tstatus=%d\tbody=redacted%n",
                uri.getHost(), response.statusCode());
    }

    private static HttpResponse<String> fetch(URI uri) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(TIMEOUT)
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static String environment(String name) {
        return System.getenv(name);
    }
}
