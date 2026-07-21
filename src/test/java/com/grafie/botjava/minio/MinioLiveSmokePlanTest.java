package com.grafie.botjava.minio;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MinioLiveSmokePlanTest {

    @Test
    void shouldRequireConfirmationCredentialsAndHttpsPublicDomain() {
        Map<String, String> missingConfirmation = validEnvironment();
        missingConfirmation.remove("MINIO_LIVE_CONFIRM");
        assertThrows(IllegalArgumentException.class,
                () -> MinioLiveSmokePlan.from(missingConfirmation));

        Map<String, String> missingSecret = validEnvironment();
        missingSecret.remove("MINIO_SECRET_KEY");
        assertThrows(IllegalArgumentException.class,
                () -> MinioLiveSmokePlan.from(missingSecret));

        Map<String, String> insecurePublicUrl = validEnvironment();
        insecurePublicUrl.put("MINIO_PUBLIC_URL", "http://media.example.com");
        assertThrows(IllegalArgumentException.class,
                () -> MinioLiveSmokePlan.from(insecurePublicUrl));
    }

    @Test
    void shouldSeparateStorageEndpointFromPublicMediaDomain() {
        MinioLiveSmokePlan.Plan plan = MinioLiveSmokePlan.from(validEnvironment());

        assertEquals("http://minio.internal:9000", plan.endpoint());
        assertEquals("https://media.example.com", plan.publicUrl());
        assertEquals("qbot", plan.bucket());
    }

    private static Map<String, String> validEnvironment() {
        Map<String, String> values = new HashMap<>();
        values.put("MINIO_LIVE_CONFIRM", MinioLiveSmokePlan.CONFIRMATION);
        values.put("MINIO_ENDPOINT", "http://minio.internal:9000/");
        values.put("MINIO_PUBLIC_URL", "https://media.example.com/");
        values.put("MINIO_BUCKET", "qbot");
        values.put("MINIO_ACCESS_KEY", "access");
        values.put("MINIO_SECRET_KEY", "secret");
        return values;
    }
}
