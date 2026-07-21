package com.grafie.botjava.minio;

import java.net.URI;
import java.util.Map;

final class MinioLiveSmokePlan {

    static final String CONFIRMATION = "UPLOAD_AND_FETCH_TEST_IMAGE";

    private MinioLiveSmokePlan() {
    }

    static Plan from(Map<String, String> environment) {
        if (!CONFIRMATION.equals(environment.get("MINIO_LIVE_CONFIRM"))) {
            throw new IllegalArgumentException(
                    "MINIO_LIVE_CONFIRM must equal " + CONFIRMATION);
        }
        String endpoint = requireHttpUrl(environment, "MINIO_ENDPOINT", false);
        String publicUrl = requireHttpUrl(environment, "MINIO_PUBLIC_URL", true);
        String bucket = required(environment, "MINIO_BUCKET");
        required(environment, "MINIO_ACCESS_KEY");
        required(environment, "MINIO_SECRET_KEY");
        return new Plan(endpoint, publicUrl, bucket);
    }

    private static String requireHttpUrl(Map<String, String> environment, String name,
                                         boolean requireHttps) {
        String value = required(environment, name);
        URI uri;
        try {
            uri = URI.create(value);
        }
        catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(name + " must be a valid URL", exception);
        }
        boolean validScheme = "https".equalsIgnoreCase(uri.getScheme())
                || (!requireHttps && "http".equalsIgnoreCase(uri.getScheme()));
        if (!validScheme || uri.getHost() == null || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException(name + (requireHttps
                    ? " must be a valid HTTPS base URL"
                    : " must be a valid HTTP(S) base URL"));
        }
        return value.replaceAll("/+$", "");
    }

    private static String required(Map<String, String> environment, String name) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required environment variable: " + name);
        }
        return value.trim();
    }

    record Plan(String endpoint, String publicUrl, String bucket) {
    }
}
