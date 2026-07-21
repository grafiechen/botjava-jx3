package com.grafie.botjava.jx3.http.command;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * 一次正则匹配得到的只读指令参数，并兼容现有命名组。
 */
public final class CommandArguments {

    private final Map<String, String> values;

    private CommandArguments(Map<String, String> values) {
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public static CommandArguments of(Map<String, String> values) {
        return new CommandArguments(values == null ? Map.of() : values);
    }

    public String get(String name) {
        return clean(values.get(name));
    }

    public String server(String defaultServer) {
        return firstNotBlank(get("server"), get("server1"), defaultServer);
    }

    public String roleName() {
        return firstNotBlank(get("roleName"), get("value1"), get("value"));
    }

    public String school() {
        return get("school");
    }

    public String value() {
        return firstNotBlank(get("value"), get("value1"));
    }

    public String keyword() {
        return get("value");
    }

    public String loop() {
        return get("loop");
    }

    public OptionalInt integer(String name, int minimum, int maximum) {
        String value = get(name);
        if (value == null) {
            return OptionalInt.empty();
        }
        final int parsed;
        try {
            parsed = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new CommandArgumentException("参数 " + name + " 必须是整数");
        }
        if (parsed < minimum || parsed > maximum) {
            throw new CommandArgumentException("参数 " + name + " 必须在 " + minimum + " 到 " + maximum + " 之间");
        }
        return OptionalInt.of(parsed);
    }

    public OptionalInt integerOneOf(String name, int... allowedValues) {
        String value = get(name);
        if (value == null) {
            return OptionalInt.empty();
        }
        final int parsed;
        try {
            parsed = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new CommandArgumentException("参数 " + name + " 必须是整数");
        }
        for (int allowedValue : allowedValues) {
            if (parsed == allowedValue) {
                return OptionalInt.of(parsed);
            }
        }
        throw new CommandArgumentException("参数 " + name + " 不在允许范围内");
    }

    public OptionalLong longInteger(String name, long minimum, long maximum) {
        String value = get(name);
        if (value == null) {
            return OptionalLong.empty();
        }
        final long parsed;
        try {
            parsed = Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new CommandArgumentException("参数 " + name + " 必须是整数");
        }
        if (parsed < minimum || parsed > maximum) {
            throw new CommandArgumentException("参数 " + name + " 超出允许范围");
        }
        return OptionalLong.of(parsed);
    }

    public Map<String, String> asMap() {
        return values;
    }

    public CommandArguments withDefaults(String server, String roleName) {
        return withDefaults(server, roleName, null);
    }

    public CommandArguments withDefaults(String server, String roleName, String school) {
        Map<String, String> merged = new LinkedHashMap<>(values);
        if (server(null) == null && clean(server) != null) {
            merged.put("server", clean(server));
        }
        if (roleName() == null && clean(roleName) != null) {
            merged.put("roleName", clean(roleName));
        }
        if (school() == null && clean(school) != null) {
            merged.put("school", clean(school));
        }
        return new CommandArguments(merged);
    }

    private static String firstNotBlank(String... candidates) {
        for (String candidate : candidates) {
            String value = clean(candidate);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }
}
