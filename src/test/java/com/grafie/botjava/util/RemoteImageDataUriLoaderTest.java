package com.grafie.botjava.util;

import com.grafie.botjava.jx3.config.RemoteImageProperties;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoteImageDataUriLoaderTest {

    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");

    @Test
    void shouldConvertValidatedImageToDataUri() {
        RemoteImageDataUriLoader loader = loader((uri, maxBytes, timeout) ->
                new RemoteImageDataUriLoader.FetchResult(200, "image/png; charset=binary", PNG.length, PNG));

        String value = loader.load("https://www.jx3api.com/cache/card.png").orElseThrow();

        assertTrue(value.startsWith("data:image/png;base64,"));
    }

    @Test
    void shouldRejectNonHttpsAndHostsOutsideAllowlistBeforeDownload() {
        AtomicInteger calls = new AtomicInteger();
        RemoteImageDataUriLoader loader = loader((uri, maxBytes, timeout) -> {
            calls.incrementAndGet();
            return new RemoteImageDataUriLoader.FetchResult(200, "image/png", PNG.length, PNG);
        });

        assertTrue(loader.load("http://www.jx3api.com/card.png").isEmpty());
        assertTrue(loader.load("https://127.0.0.1/card.png").isEmpty());
        assertTrue(loader.load("https://jx3api.com.evil.example/card.png").isEmpty());
        assertEquals(0, calls.get());
    }

    @Test
    void shouldRejectRedirectsOversizeAndUnsupportedMedia() {
        assertTrue(loader((uri, maxBytes, timeout) ->
                new RemoteImageDataUriLoader.FetchResult(302, "image/png", PNG.length, PNG))
                .load("https://www.jx3api.com/card.png").isEmpty());
        assertTrue(loader((uri, maxBytes, timeout) ->
                new RemoteImageDataUriLoader.FetchResult(200, "image/png", 6 * 1024 * 1024L, PNG))
                .load("https://www.jx3api.com/card.png").isEmpty());
        assertTrue(loader((uri, maxBytes, timeout) ->
                new RemoteImageDataUriLoader.FetchResult(200, "image/svg+xml", PNG.length, PNG))
                .load("https://www.jx3api.com/card.png").isEmpty());
    }

    @Test
    void shouldRejectContentTypeAndSignatureMismatch() {
        RemoteImageDataUriLoader loader = loader((uri, maxBytes, timeout) ->
                new RemoteImageDataUriLoader.FetchResult(200, "image/jpeg", PNG.length, PNG));

        assertTrue(loader.load("https://www.jx3api.com/card.jpg").isEmpty());
    }

    private RemoteImageDataUriLoader loader(RemoteImageDataUriLoader.ImageFetcher fetcher) {
        RemoteImageProperties properties = new RemoteImageProperties();
        properties.setAllowedHosts(Set.of("www.jx3api.com"));
        return new RemoteImageDataUriLoader(properties, fetcher);
    }
}
