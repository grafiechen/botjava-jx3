package com.grafie.botjava.util;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import org.springframework.http.ResponseEntity;

/**
 * @author grafie.chen
 * @since 2025/1/23  11:22
 */
@Slf4j
public class RequestUtil {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    /**
     * 执行post请求。默认 Content-Type 为 application/json。
     * header只允许是String
     *
     * @param baseUrl    基础地址
     * @param url        请求url
     * @param param      请求参数
     * @param headersMap header
     * @return 接口返回的string字符串，根据接口内容
     */
    public static <T> T doPost(String baseUrl, String url, Map<String, Object> param,
                               Map<String, String> headersMap, Class<T> clazz) {
        return doPost(baseUrl, url, param, headersMap, clazz, DEFAULT_TIMEOUT);
    }

    public static <T> T doPost(String baseUrl, String url, Map<String, Object> param,
                               Map<String, String> headersMap, Class<T> clazz, Duration timeout) {
        return doRequest(HttpMethod.POST, baseUrl, url, Collections.emptyMap(),
                param, headersMap, clazz, timeout);
    }

    /**
     * 执行通用 JSON HTTP 请求。GET/DELETE 使用 queryParam，POST/PUT 使用 requestBody。
     */
    public static <T> T doRequest(HttpMethod method, String baseUrl, String url,
                                  Map<String, Object> queryParam, Map<String, Object> requestBody,
                                  Map<String, String> headersMap, Class<T> clazz, Duration timeout) {
        if (method == null) {
            throw new IllegalArgumentException("HTTP method 不能为空");
        }
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("HTTP timeout 必须大于 0");
        }
        OutboundHttpRateLimiter.awaitPermit();
        long startNanos = System.nanoTime();
        log.info("外部 HTTP 请求开始，method=>{}，baseUrl=>{}，path=>{}，query=>{}，body=>{}，headers=>{}",
                method, baseUrl, url, SensitiveDataUtil.redact(queryParam),
                SensitiveDataUtil.redact(requestBody), SensitiveDataUtil.redact(headersMap));
        WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();
        try {
            WebClient.RequestBodySpec request = webClient.method(method)
                    .uri(uriBuilder -> {
                        if (url != null && !url.isBlank()) {
                            uriBuilder.path(url);
                        }
                        if (queryParam != null) {
                            queryParam.forEach((key, value) -> {
                                if (key != null && !key.isBlank() && value != null) {
                                    uriBuilder.queryParam(key, value);
                                }
                            });
                        }
                        return uriBuilder.build();
                    });
            if (headersMap != null) {
                request.headers(headers -> headersMap.forEach(headers::add));
            }
            request.accept(MediaType.APPLICATION_JSON);
            WebClient.RequestHeadersSpec<?> requestHeaders = requestBody == null
                    ? request
                    : request.contentType(MediaType.APPLICATION_JSON).bodyValue(requestBody);
            ResponseEntity<T> response = requestHeaders.retrieve()
                    .toEntity(clazz)
                    .block(timeout);
            T result = response == null ? null : response.getBody();
            log.info("外部 HTTP 请求完成，method=>{}，baseUrl=>{}，path=>{}，status=>{}，elapsedMs=>{}，responseType=>{}",
                    method, baseUrl, url, response == null ? null : response.getStatusCode().value(),
                    elapsedMillis(startNanos),
                    result == null ? null : result.getClass().getSimpleName());
            return result;
        } catch (WebClientResponseException e) {
            log.error("外部 HTTP 请求返回错误，method=>{}，baseUrl=>{}，path=>{}，query=>{}，body=>{}，headers=>{}，status=>{}，elapsedMs=>{}，responseBody=>{}",
                    method, baseUrl, url, SensitiveDataUtil.redact(queryParam),
                    SensitiveDataUtil.redact(requestBody), SensitiveDataUtil.redact(headersMap),
                    e.getStatusCode().value(), elapsedMillis(startNanos),
                    SensitiveDataUtil.redactText(e.getResponseBodyAsString()), e);
            throw new RemoteHttpException(
                    e.getStatusCode().value(), e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("外部 HTTP 请求失败，method=>{}，baseUrl=>{}，path=>{}，query=>{}，body=>{}，headers=>{}，elapsedMs=>{}，reason=>{}",
                    method, baseUrl, url, SensitiveDataUtil.redact(queryParam),
                    SensitiveDataUtil.redact(requestBody), SensitiveDataUtil.redact(headersMap),
                    elapsedMillis(startNanos), SensitiveDataUtil.summarize(e), e);
            throw new RuntimeException("远程 HTTP 请求失败", e);
        }
    }

    private static long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
