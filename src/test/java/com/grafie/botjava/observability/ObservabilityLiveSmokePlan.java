package com.grafie.botjava.observability;

import java.net.URI;
import java.util.Map;

final class ObservabilityLiveSmokePlan {

    private ObservabilityLiveSmokePlan() {
    }

    static Plan from(Map<String, String> environment) {
        requireEquals(environment.get("OBSERVABILITY_LIVE_SMOKE"), "true",
                "OBSERVABILITY_LIVE_SMOKE must equal true");
        URI prometheusUri = requireHttpUrl(environment, "OBSERVABILITY_PROMETHEUS_URL");
        return new Plan(prometheusUri);
    }

    private static URI requireHttpUrl(Map<String, String> environment, String name) {
        String value = required(environment, name);
        URI uri;
        try {
            uri = URI.create(value);
        }
        catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(name + " must be a valid HTTP or HTTPS URL", exception);
        }
        String scheme = uri.getScheme();
        if (uri.getHost() == null
                || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            throw new IllegalArgumentException(name + " must be a valid HTTP or HTTPS URL");
        }
        return uri;
    }

    private static String required(Map<String, String> environment, String name) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required environment variable: " + name);
        }
        return value.trim();
    }

    private static void requireEquals(String actual, String expected, String message) {
        if (!expected.equalsIgnoreCase(actual == null ? "" : actual.trim())) {
            throw new IllegalArgumentException(message);
        }
    }

    record Plan(URI prometheusUri) {
    }
}
