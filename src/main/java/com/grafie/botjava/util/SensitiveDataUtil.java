package com.grafie.botjava.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 日志参数脱敏工具。
 */
public final class SensitiveDataUtil {

    private static final String MASK = "******";
    private static final Set<String> SENSITIVE_KEY_PARTS = Set.of(
            "token", "secret", "ticket", "password", "authorization", "accesskey", "privatekey", "appkey"
    );
    private static final Pattern SENSITIVE_TEXT_VALUE = Pattern.compile(
            "(?i)(token|ticket|secret|password|authorization|access[_-]?key|appkey)\\s*[=:]\\s*[^\\s,;]+"
    );

    private SensitiveDataUtil() {
    }

    public static Map<String, Object> redact(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (source == null) {
            return result;
        }
        source.forEach((key, value) -> {
            String keyText = String.valueOf(key);
            result.put(keyText, isSensitiveKey(keyText) ? MASK : redactValue(value));
        });
        return result;
    }

    public static String redactText(String source) {
        if (source == null) {
            return null;
        }
        return SENSITIVE_TEXT_VALUE.matcher(source).replaceAll("$1=******");
    }

    public static String summarize(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        String summary = throwable.getClass().getSimpleName();
        if (throwable.getMessage() != null && !throwable.getMessage().isBlank()) {
            summary += ": " + redactText(throwable.getMessage().replace('\r', ' ').replace('\n', ' ').trim());
        }
        return summary;
    }

    private static Object redactValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return redact(map);
        }
        if (value instanceof List<?> list) {
            List<Object> result = new ArrayList<>(list.size());
            list.forEach(item -> result.add(redactValue(item)));
            return result;
        }
        return value;
    }

    private static boolean isSensitiveKey(String key) {
        String normalized = key.replace("-", "").replace("_", "").toLowerCase(Locale.ROOT);
        return SENSITIVE_KEY_PARTS.stream().anyMatch(normalized::contains);
    }
}
