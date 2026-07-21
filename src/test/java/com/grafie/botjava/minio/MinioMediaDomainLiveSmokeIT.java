package com.grafie.botjava.minio;

import com.grafie.botjava.util.HtmlToImageUtl;
import io.minio.MinioClient;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Explicit end-to-end media-domain acceptance runner. It leaves one uniquely
 * named public test object in the configured bucket for later QQ IMAGE smoke.
 */
class MinioMediaDomainLiveSmokeIT {

    private static final byte[] PNG_SIGNATURE = new byte[]{
            (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
    };
    private static final int MAX_RESPONSE_BYTES = 5 * 1024 * 1024;

    @Test
    void shouldRenderUploadAndFetchPngThroughPublicHttpsDomain() throws Exception {
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("MINIO_LIVE_SMOKE")),
                "Set MINIO_LIVE_SMOKE=true to upload a public test image");
        Map<String, String> environment = System.getenv();
        MinioLiveSmokePlan.Plan plan = MinioLiveSmokePlan.from(environment);

        Path image = Path.of(HtmlToImageUtl.renderTemplateToImage(
                "搜索区服",
                Map.of(
                        "query", "媒体域名验收",
                        "data", Map.of(
                                "zone", "测试大区",
                                "name", "测试服务器",
                                "center", "测试中心",
                                "aliases", List.of("验收别名"),
                                "slaves", List.of("验收子服"))
                )));
        assertTrue(Files.isRegularFile(image));
        assertTrue(Files.size(image) > PNG_SIGNATURE.length);

        MinioClient client = MinioClient.builder()
                .endpoint(plan.endpoint())
                .credentials(requiredEnvironment("MINIO_ACCESS_KEY"),
                        requiredEnvironment("MINIO_SECRET_KEY"))
                .build();
        String publicUrl = new MinioUtil(client, plan.bucket(), plan.publicUrl())
                .uploadFile(image.toFile(), "botjava-media-smoke.png");
        assertTrue(publicUrl.startsWith(plan.publicUrl() + "/"));

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(publicUrl))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        HttpResponse<byte[]> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofByteArray());

        assertEquals(200, response.statusCode());
        String contentType = response.headers().firstValue("Content-Type").orElse("");
        assertTrue(contentType.toLowerCase().startsWith("image/png"), contentType);
        assertTrue(response.body().length > PNG_SIGNATURE.length);
        assertTrue(response.body().length <= MAX_RESPONSE_BYTES);
        assertArrayEquals(PNG_SIGNATURE,
                java.util.Arrays.copyOf(response.body(), PNG_SIGNATURE.length));
        System.out.println("MINIO_MEDIA\tUPLOAD_FETCH\tSUCCESS");
        System.out.println("QQ_GROUP_LIVE_IMAGE_URL=" + publicUrl);
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }
        return value.trim();
    }
}
