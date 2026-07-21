package com.grafie.botjava.observability;

import com.grafie.botjava.entity.CommandInvocationStatus;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.http.util.REGEX;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 机器人公共链路指标。标签只能使用代码内有限枚举，不得加入用户或群标识。
 */
@Slf4j
@Component
public class BotMetrics {

    private static final String NONE = "none";
    private final MeterRegistry meterRegistry;

    public BotMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordCommand(REGEX definition, CommandInvocationStatus status,
                              BotResponse.ResponseType responseType, long elapsedMillis) {
        safeRecord("群指令", () -> Timer.builder("bot.command.duration")
                .description("群指令从匹配到处理结束的耗时")
                .tag("command", definition.name())
                .tag("command_group", definition.getCommandGroup().name().toLowerCase())
                .tag("status", status.name().toLowerCase())
                .tag("response_type", responseType == null ? NONE : responseType.name().toLowerCase())
                .tag("external_call", Boolean.toString(definition.usesExternalCall()))
                .register(meterRegistry)
                .record(Duration.ofMillis(Math.max(0, elapsedMillis))));
    }

    public void recordJx3Request(String path, String source, String outcome, long elapsedNanos) {
        safeRecord("JX3API", () -> Timer.builder("bot.jx3.request.duration")
                .description("JX3API 查询耗时，区分远端请求与本地缓存")
                .tag("path", safeTag(path))
                .tag("source", safeTag(source))
                .tag("outcome", safeTag(outcome))
                .register(meterRegistry)
                .record(Duration.ofNanos(Math.max(0, elapsedNanos))));
    }

    public void recordJx3Cache(String path, String result) {
        safeRecord("JX3API 缓存", () -> Counter.builder("bot.jx3.cache.requests")
                .description("可缓存 JX3API 查询的命中和未命中次数")
                .tag("path", safeTag(path))
                .tag("result", safeTag(result))
                .register(meterRegistry)
                .increment());
    }

    public void recordQqRequest(String operation, String outcome, long elapsedNanos) {
        safeRecord("QQ OpenAPI", () -> Timer.builder("bot.qq.request.duration")
                .description("QQ 群消息发送与媒体上传耗时")
                .tag("operation", safeTag(operation))
                .tag("outcome", safeTag(outcome))
                .register(meterRegistry)
                .record(Duration.ofNanos(Math.max(0, elapsedNanos))));
    }

    private String safeTag(String value) {
        return value == null || value.isBlank() ? NONE : value;
    }

    private void safeRecord(String metricName, Runnable recorder) {
        try {
            recorder.run();
        } catch (RuntimeException exception) {
            log.warn("记录{}指标失败，reason=>{}", metricName, exception.getClass().getSimpleName());
        }
    }
}
