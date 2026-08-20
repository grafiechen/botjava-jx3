package com.grafie.botjava.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * 进程级外部网络请求启动限速器。所有 HTTP 请求和 WebSocket 握手共享同一时间线。
 */
@Component
public class OutboundHttpRateLimiter {
    private static final Object LOCK = new Object();
    private static volatile boolean enabled;
    private static volatile long intervalNanos = TimeUnit.MILLISECONDS.toNanos(500);
    private static long nextPermitNanos;

    public OutboundHttpRateLimiter(
            @Value("${bot.outbound-http.rate-limit.enabled:true}") boolean configuredEnabled,
            @Value("${bot.outbound-http.rate-limit.max-requests-per-second:2}") int maxRequestsPerSecond) {
        configure(configuredEnabled, maxRequestsPerSecond);
    }

    public static void awaitPermit() {
        if (!enabled) {
            return;
        }
        long waitNanos;
        synchronized (LOCK) {
            long now = System.nanoTime();
            long permitAt = Math.max(now, nextPermitNanos);
            nextPermitNanos = permitAt + intervalNanos;
            waitNanos = permitAt - now;
        }
        while (waitNanos > 0) {
            long started = System.nanoTime();
            LockSupport.parkNanos(waitNanos);
            if (Thread.interrupted()) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("等待外部请求限速许可时线程被中断");
            }
            waitNanos -= Math.max(0, System.nanoTime() - started);
        }
    }

    static void configure(boolean configuredEnabled, int maxRequestsPerSecond) {
        if (maxRequestsPerSecond < 1 || maxRequestsPerSecond > 2) {
            throw new IllegalArgumentException("外部请求每秒上限必须在 1 到 2 之间");
        }
        synchronized (LOCK) {
            enabled = configuredEnabled;
            intervalNanos = TimeUnit.SECONDS.toNanos(1) / maxRequestsPerSecond;
            nextPermitNanos = 0;
        }
    }
}