package com.grafie.botjava.observability;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ObservabilityLiveSmokePlanTest {

    @Test
    void shouldRequireExplicitConfirmation() {
        Map<String, String> values = base();
        values.remove("OBSERVABILITY_LIVE_SMOKE");

        assertThrows(IllegalArgumentException.class,
                () -> ObservabilityLiveSmokePlan.from(values));
    }

    @Test
    void shouldAcceptHttpAndHttpsPrometheusUrls() {
        ObservabilityLiveSmokePlan.Plan http = ObservabilityLiveSmokePlan.from(base());
        assertEquals("http", http.prometheusUri().getScheme());
        assertEquals("127.0.0.1", http.prometheusUri().getHost());

        Map<String, String> httpsValues = base();
        httpsValues.put("OBSERVABILITY_PROMETHEUS_URL",
                "https://metrics.example.com/actuator/prometheus");
        ObservabilityLiveSmokePlan.Plan https = ObservabilityLiveSmokePlan.from(httpsValues);
        assertEquals("https", https.prometheusUri().getScheme());
        assertEquals("metrics.example.com", https.prometheusUri().getHost());
    }

    @Test
    void shouldRejectMissingOrUnsupportedPrometheusUrls() {
        Map<String, String> missing = base();
        missing.remove("OBSERVABILITY_PROMETHEUS_URL");
        assertThrows(IllegalArgumentException.class,
                () -> ObservabilityLiveSmokePlan.from(missing));

        Map<String, String> unsupported = base();
        unsupported.put("OBSERVABILITY_PROMETHEUS_URL", "file:///tmp/prometheus.txt");
        assertThrows(IllegalArgumentException.class,
                () -> ObservabilityLiveSmokePlan.from(unsupported));
    }

    private static Map<String, String> base() {
        Map<String, String> values = new HashMap<>();
        values.put("OBSERVABILITY_LIVE_SMOKE", "true");
        values.put("OBSERVABILITY_PROMETHEUS_URL", "http://127.0.0.1:8082/actuator/prometheus");
        return values;
    }
}
