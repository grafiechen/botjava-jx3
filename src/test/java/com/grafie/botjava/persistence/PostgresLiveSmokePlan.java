package com.grafie.botjava.persistence;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

final class PostgresLiveSmokePlan {

    static final String CONFIRMATION = "USE_DEDICATED_SMOKE_SCHEMA";

    private PostgresLiveSmokePlan() {
    }

    static Plan from(Map<String, String> environment) {
        if (!CONFIRMATION.equals(environment.get("POSTGRES_LIVE_CONFIRM"))) {
            throw new IllegalArgumentException(
                    "POSTGRES_LIVE_CONFIRM must equal " + CONFIRMATION);
        }
        String url = required(environment, "POSTGRES_LIVE_JDBC_URL");
        String username = required(environment, "POSTGRES_LIVE_USERNAME");
        required(environment, "POSTGRES_LIVE_PASSWORD");
        String schema = required(environment, "POSTGRES_LIVE_SCHEMA");
        if (!schema.matches("botjava_smoke_[a-z0-9_]{1,40}")) {
            throw new IllegalArgumentException(
                    "POSTGRES_LIVE_SCHEMA must match botjava_smoke_[a-z0-9_]{1,40}");
        }
        if (!url.startsWith("jdbc:postgresql://")) {
            throw new IllegalArgumentException("POSTGRES_LIVE_JDBC_URL must use jdbc:postgresql");
        }
        URI uri;
        try {
            uri = URI.create(url.substring("jdbc:".length()));
        }
        catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("POSTGRES_LIVE_JDBC_URL is invalid", exception);
        }
        String currentSchema = queryValue(uri.getRawQuery(), "currentSchema");
        if (!schema.equals(currentSchema)) {
            throw new IllegalArgumentException(
                    "POSTGRES_LIVE_JDBC_URL currentSchema must equal POSTGRES_LIVE_SCHEMA");
        }
        return new Plan(url, username, schema);
    }

    private static String queryValue(String rawQuery, String name) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return null;
        }
        return Arrays.stream(rawQuery.split("&"))
                .map(pair -> pair.split("=", 2))
                .filter(pair -> pair.length == 2 && name.equalsIgnoreCase(decode(pair[0])))
                .map(pair -> decode(pair[1]))
                .findFirst()
                .orElse(null);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String required(Map<String, String> environment, String name) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required environment variable: " + name);
        }
        return value.trim();
    }

    record Plan(String jdbcUrl, String username, String schema) {
    }
}
