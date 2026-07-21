package com.grafie.botjava.util;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

/**
 * @author grafie.chen
 * @since 2025/1/23  11:22
 */
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
            return requestHeaders.retrieve()
                    .bodyToMono(clazz)
                    .block(timeout);
        } catch (WebClientResponseException e) {
            throw new RemoteHttpException(
                    e.getStatusCode().value(), e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new RuntimeException("远程 HTTP 请求失败", e);
        }
    }
}
