package com.grafie.botjava.observability;

import com.grafie.botjava.entity.CommandInvocationStatus;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.http.util.REGEX;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BotMetricsTest {

    @Test
    void shouldRecordBoundedCommandJx3AndQqMetrics() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        BotMetrics metrics = new BotMetrics(registry);

        metrics.recordCommand(REGEX.ServerCheck, CommandInvocationStatus.SUCCESS,
                BotResponse.ResponseType.TEXT, 42);
        metrics.recordJx3Request("/data/status/check", "remote", "success", 5_000_000);
        metrics.recordJx3Cache("/data/status/check", "hit");
        metrics.recordJx3Cache("/data/status/check", "miss");
        metrics.recordQqRequest("send_message", "success", 3_000_000);

        assertEquals(1, registry.get("bot.command.duration")
                .tags("command", "ServerCheck", "status", "success")
                .timer().count());
        assertEquals(1, registry.get("bot.jx3.request.duration")
                .tags("path", "/data/status/check", "source", "remote", "outcome", "success")
                .timer().count());
        assertEquals(1, registry.get("bot.qq.request.duration")
                .tags("operation", "send_message", "outcome", "success")
                .timer().count());
        assertEquals(1, registry.get("bot.jx3.cache.requests")
                .tags("path", "/data/status/check", "result", "hit")
                .counter().count());
        assertEquals(1, registry.get("bot.jx3.cache.requests")
                .tags("path", "/data/status/check", "result", "miss")
                .counter().count());

        assertNull(registry.find("bot.command.duration").tag("group_openid", "group-1").timer());
        assertNull(registry.find("bot.command.duration").tag("member_openid", "member-1").timer());
    }
}
