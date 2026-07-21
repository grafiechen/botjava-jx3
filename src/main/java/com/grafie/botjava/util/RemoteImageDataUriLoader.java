package com.grafie.botjava.util;

import com.grafie.botjava.jx3.config.RemoteImageProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Iterator;
import java.util.Locale;
import java.util.Optional;

@Component
@Slf4j
public class RemoteImageDataUriLoader {

    private final RemoteImageProperties properties;
    private final ImageFetcher fetcher;

    public RemoteImageDataUriLoader(RemoteImageProperties properties) {
        this(properties, new JdkImageFetcher(properties));
    }

    RemoteImageDataUriLoader(RemoteImageProperties properties, ImageFetcher fetcher) {
        this.properties = properties;
        this.fetcher = fetcher;
    }

    public Optional<String> load(String rawUrl) {
        if (!properties.isEnabled() || rawUrl == null || rawUrl.isBlank()) {
            return Optional.empty();
        }
        URI uri;
        try {
            uri = URI.create(rawUrl.trim());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getPort() != -1 || !allowed(uri.getHost())) {
            return Optional.empty();
        }
        try {
            FetchResult result = fetcher.fetch(uri, properties.getMaxBytes(),
                    Duration.ofSeconds(properties.getTimeoutSeconds()));
            if (result.statusCode() != 200 || result.contentLength() > properties.getMaxBytes()
                    || result.bytes() == null || result.bytes().length == 0
                    || result.bytes().length > properties.getMaxBytes()) {
                return Optional.empty();
            }
            String mediaType = canonicalMediaType(result.contentType());
            if (mediaType == null || !matchesSignature(mediaType, result.bytes())
                    || !withinPixelLimit(result.bytes())) {
                return Optional.empty();
            }
            return Optional.of("data:" + mediaType + ";base64,"
                    + Base64.getEncoder().encodeToString(result.bytes()));
        } catch (Exception e) {
            log.info("远程名片图片加载失败，host=>{}，reason=>{}", uri.getHost(),
                    SensitiveDataUtil.summarize(e));
            return Optional.empty();
        }
    }

    private boolean allowed(String host) {
        return properties.getAllowedHosts().stream()
                .filter(value -> value != null)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.equals(host.toLowerCase(Locale.ROOT)));
    }

    private String canonicalMediaType(String contentType) {
        if (contentType == null) {
            return null;
        }
        String value = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "image/png" -> "image/png";
            case "image/jpeg", "image/jpg" -> "image/jpeg";
            case "image/gif" -> "image/gif";
            default -> null;
        };
    }

    private boolean matchesSignature(String mediaType, byte[] bytes) {
        return switch (mediaType) {
            case "image/png" -> startsWith(bytes, new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});
            case "image/jpeg" -> bytes.length >= 3 && (bytes[0] & 0xff) == 0xff
                    && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff;
            case "image/gif" -> startsWith(bytes, "GIF87a".getBytes(StandardCharsets.US_ASCII))
                    || startsWith(bytes, "GIF89a".getBytes(StandardCharsets.US_ASCII));
            default -> false;
        };
    }

    private boolean startsWith(byte[] bytes, byte[] signature) {
        if (bytes.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if (bytes[index] != signature[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean withinPixelLimit(byte[] bytes) throws Exception {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) {
                return false;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                return false;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                return width > 0 && height > 0
                        && (long) width * height <= properties.getMaxPixels();
            } finally {
                reader.dispose();
            }
        }
    }

    @FunctionalInterface
    interface ImageFetcher {
        FetchResult fetch(URI uri, int maxBytes, Duration timeout) throws Exception;
    }

    record FetchResult(int statusCode, String contentType, long contentLength, byte[] bytes) {
    }

    private static final class JdkImageFetcher implements ImageFetcher {
        private final HttpClient httpClient;

        private JdkImageFetcher(RemoteImageProperties properties) {
            this.httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build();
        }

        @Override
        public FetchResult fetch(URI uri, int maxBytes, Duration timeout) throws Exception {
            rejectPrivateAddress(uri.getHost());
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(timeout)
                    .header("Accept", "image/png,image/jpeg,image/gif")
                    .header("User-Agent", "botjava-jx3/1.0")
                    .GET()
                    .build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
            String contentType = response.headers().firstValue("Content-Type").orElse(null);
            try (InputStream body = response.body()) {
                if (contentLength > maxBytes) {
                    return new FetchResult(response.statusCode(), contentType, contentLength, new byte[0]);
                }
                return new FetchResult(response.statusCode(), contentType, contentLength,
                        readBounded(body, maxBytes));
            }
        }

        private static byte[] readBounded(InputStream input, int maxBytes) throws Exception {
            ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(maxBytes, 64 * 1024));
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > maxBytes) {
                    throw new IllegalArgumentException("远程图片超过大小限制");
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }

        private static void rejectPrivateAddress(String host) throws Exception {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            if (addresses.length == 0) {
                throw new IllegalArgumentException("远程图片域名无法解析");
            }
            for (InetAddress address : addresses) {
                byte[] raw = address.getAddress();
                boolean uniqueLocalIpv6 = raw.length == 16 && (raw[0] & 0xfe) == 0xfc;
                if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                        || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                        || address.isMulticastAddress() || uniqueLocalIpv6) {
                    throw new IllegalArgumentException("远程图片地址不允许访问内网");
                }
            }
        }
    }
}
