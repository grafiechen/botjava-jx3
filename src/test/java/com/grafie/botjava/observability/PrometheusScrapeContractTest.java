package com.grafie.botjava.observability;

import com.grafie.botjava.entity.CommandInvocationStatus;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.http.util.REGEX;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrometheusScrapeContractTest {

    @Test
    void shouldExposeBotMetricsWithOnlyBoundedLabels() {
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        BotMetrics metrics = new BotMetrics(registry);

        metrics.recordCommand(REGEX.ServerCheck, CommandInvocationStatus.SUCCESS,
                BotResponse.ResponseType.TEXT, 42);
        metrics.recordJx3Request("/data/status/check", "remote", "success", 5_000_000);
        metrics.recordJx3Cache("/data/status/check", "hit");
        metrics.recordQqRequest("send_message", "success", 3_000_000);

        String scrape = registry.scrape();

        assertTrue(scrape.contains("bot_command_duration_seconds"));
        assertTrue(scrape.contains("command=\"ServerCheck\""));
        assertTrue(scrape.contains("command_group=\"free\""));
        assertTrue(scrape.contains("response_type=\"text\""));
        assertTrue(scrape.contains("external_call=\"true\""));
        assertTrue(scrape.contains("bot_jx3_request_duration_seconds"));
        assertTrue(scrape.contains("path=\"/data/status/check\""));
        assertTrue(scrape.contains("source=\"remote\""));
        assertTrue(scrape.contains("bot_jx3_cache_requests_total"));
        assertTrue(scrape.contains("result=\"hit\""));
        assertTrue(scrape.contains("bot_qq_request_duration_seconds"));
        assertTrue(scrape.contains("operation=\"send_message\""));

        assertFalse(scrape.contains("group_openid"));
        assertFalse(scrape.contains("member_openid"));
        assertFalse(scrape.contains("message_id"));
        assertFalse(scrape.contains("role_name"));
        assertFalse(scrape.contains("content="));
    }
}
