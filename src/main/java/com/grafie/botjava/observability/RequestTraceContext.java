package com.grafie.botjava.observability;

import org.slf4j.MDC;

import java.util.Optional;

/**
 * 同步消息处理链路的可嵌套追踪上下文。
 */
public final class RequestTraceContext {

    public static final String MDC_KEY = "invocationId";
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private RequestTraceContext() {
    }

    public static Scope open(String invocationId) {
        if (invocationId == null || invocationId.isBlank()) {
            throw new IllegalArgumentException("invocationId 不能为空");
        }
        String normalized = invocationId.trim();
        String previous = CURRENT.get();
        CURRENT.set(normalized);
        MDC.put(MDC_KEY, normalized);
        return new Scope(previous);
    }

    public static Optional<String> currentId() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static final class Scope implements AutoCloseable {
        private final String previous;
        private boolean closed;

        private Scope(String previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            if (previous == null) {
                CURRENT.remove();
                MDC.remove(MDC_KEY);
            } else {
                CURRENT.set(previous);
                MDC.put(MDC_KEY, previous);
            }
        }
    }
}
