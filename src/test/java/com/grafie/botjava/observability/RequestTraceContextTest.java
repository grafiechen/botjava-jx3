package com.grafie.botjava.observability;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RequestTraceContextTest {

    @Test
    void shouldExposeInvocationIdInConfiguredLogPattern() throws IOException {
        String configuration = new ClassPathResource("application.yml")
                .getContentAsString(StandardCharsets.UTF_8);

        assertEquals(true, configuration.contains("%X{invocationId:-}"));
    }

    @Test
    void shouldRestoreNestedScopeAndClearThreadState() {
        assertFalse(RequestTraceContext.currentId().isPresent());

        try (RequestTraceContext.Scope outer = RequestTraceContext.open("invocation-outer")) {
            assertEquals("invocation-outer", RequestTraceContext.currentId().orElseThrow());
            assertEquals("invocation-outer", MDC.get(RequestTraceContext.MDC_KEY));
            try (RequestTraceContext.Scope inner = RequestTraceContext.open("invocation-inner")) {
                assertEquals("invocation-inner", RequestTraceContext.currentId().orElseThrow());
                assertEquals("invocation-inner", MDC.get(RequestTraceContext.MDC_KEY));
            }
            assertEquals("invocation-outer", RequestTraceContext.currentId().orElseThrow());
            assertEquals("invocation-outer", MDC.get(RequestTraceContext.MDC_KEY));
        }

        assertFalse(RequestTraceContext.currentId().isPresent());
        assertFalse(MDC.getCopyOfContextMap() != null
                && MDC.getCopyOfContextMap().containsKey(RequestTraceContext.MDC_KEY));
    }
}
