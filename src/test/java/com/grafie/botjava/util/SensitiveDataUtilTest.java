package com.grafie.botjava.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SensitiveDataUtilTest {

    @Test
    void shouldRedactNestedCredentialsWithoutChangingReadableParameters() {
        Map<String, Object> source = Map.of(
                "server", "乾坤一掷",
                "token", "JX3_TOKEN",
                "appkey", "ALI_APP_KEY",
                "clientSecret", "QQ_SECRET",
                "nested", Map.of("ticket", "ROLE_TICKET", "name", "加菲"),
                "items", List.of(Map.of("password", "DB_PASSWORD", "value", 1))
        );

        Map<String, Object> redacted = SensitiveDataUtil.redact(source);

        assertEquals("乾坤一掷", redacted.get("server"));
        assertEquals("******", redacted.get("token"));
        assertEquals("******", redacted.get("clientSecret"));
        assertEquals("******", redacted.get("appkey"));
        assertFalse(redacted.toString().contains("JX3_TOKEN"));
        assertFalse(redacted.toString().contains("QQ_SECRET"));
        assertFalse(redacted.toString().contains("ALI_APP_KEY"));
        assertFalse(redacted.toString().contains("ROLE_TICKET"));
        assertFalse(redacted.toString().contains("DB_PASSWORD"));
    }

    @Test
    void shouldRedactCredentialsInFreeText() {
        String redacted = SensitiveDataUtil.redactText(
                "token=real-token ticket:real-ticket server=乾坤一掷");

        assertEquals("token=****** ticket=****** server=乾坤一掷", redacted);

        String jsonRedacted = SensitiveDataUtil.redactText(
                "{\"token\":\"real-token\",\"server\":\"乾坤一掷\"}");

        assertEquals("{\"token\":\"******\",\"server\":\"乾坤一掷\"}", jsonRedacted);
    }

    @Test
    void shouldBuildSanitizedExceptionSummary() {
        String summary = SensitiveDataUtil.summarize(
                new IllegalStateException("QQ send failed token=secret-value\nretry later"));

        assertEquals("IllegalStateException: QQ send failed token=****** retry later", summary);
        assertFalse(summary.contains("secret-value"));
    }
}
