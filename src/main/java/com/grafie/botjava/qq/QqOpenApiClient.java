package com.grafie.botjava.qq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.config.TxBotProperty;
import com.grafie.botjava.entity.dto.token.AccessTokenDto;
import com.grafie.botjava.observability.RequestTraceContext;
import com.grafie.botjava.util.RemoteHttpException;
import com.grafie.botjava.util.RequestUtil;
import com.grafie.botjava.util.SensitiveDataUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * QQ OpenAPI 通用鉴权与 HTTP 传输客户端。
 *
 * 具体资源客户端负责路径和请求模型，本类统一管理 token、认证重试和错误映射。
 */
@Slf4j
@Component
public class QqOpenApiClient {

    private static final int TOKEN_EXPIRY_BUFFER_SECONDS = 60;

    private final TxBotProperty txBotProperty;
    private final ObjectMapper objectMapper;

    private LocalDateTime needGetNewTokenTime;
    private String accessToken;

    public QqOpenApiClient(TxBotProperty txBotProperty, ObjectMapper objectMapper) {
        this.txBotProperty = txBotProperty;
        this.objectMapper = objectMapper;
    }

    public <T> T get(String path, Map<String, Object> query, Class<T> responseType) {
        return request(HttpMethod.GET, path, query, null, responseType);
    }

    public <T> T post(String path, Map<String, Object> request, Class<T> responseType) {
        if (request == null) {
            throw new IllegalArgumentException("QQ OpenAPI POST 请求体不能为空");
        }
        return request(HttpMethod.POST, path, Collections.emptyMap(), request, responseType);
    }

    public <T> T put(String path, Map<String, Object> request, Class<T> responseType) {
        if (request == null) {
            throw new IllegalArgumentException("QQ OpenAPI PUT 请求体不能为空");
        }
        return request(HttpMethod.PUT, path, Collections.emptyMap(), request, responseType);
    }

    public <T> T delete(String path, Map<String, Object> query, Class<T> responseType) {
        return request(HttpMethod.DELETE, path, query, null, responseType);
    }

    protected AccessTokenDto requestAccessToken(Map<String, Object> request,
                                                Map<String, String> headers) {
        return RequestUtil.doPost(txBotProperty.getAccessTokenUrl(), null,
                request, headers, AccessTokenDto.class, requestTimeout());
    }

    protected <T> T requestOpenApi(HttpMethod method, String path, Map<String, Object> query,
                                   Map<String, Object> request, Map<String, String> headers,
                                   Class<T> responseType) {
        return RequestUtil.doRequest(method, txBotProperty.getOpenapiUrl(), path,
                query, request, headers, responseType, requestTimeout());
    }

    private <T> T request(HttpMethod method, String path, Map<String, Object> query,
                          Map<String, Object> request, Class<T> responseType) {
        if (path == null || path.isBlank() || !path.startsWith("/")) {
            throw new IllegalArgumentException("QQ OpenAPI path 必须以 / 开头");
        }
        if (responseType == null) {
            throw new IllegalArgumentException("QQ OpenAPI responseType 不能为空");
        }
        Map<String, Object> safeQuery = query == null ? Collections.emptyMap() : new HashMap<>(query);
        return executeOpenApi(() -> requestOpenApi(
                method, path, safeQuery, request, authorizationHeader(), responseType));
    }

    private <T> T executeOpenApi(RequestCall<T> requestCall) {
        for (int attempt = 0; attempt < 2; attempt++) {
            refreshToken();
            try {
                return requestCall.execute();
            } catch (RemoteHttpException e) {
                QqOpenApiException mapped = QqOpenApiErrorMapper.fromHttp(e, objectMapper);
                if (attempt == 0 && mapped.getCategory() == QqOpenApiException.Category.AUTHENTICATION) {
                    invalidateToken();
                    continue;
                }
                throw mapped;
            } catch (QqOpenApiException e) {
                throw e;
            } catch (RuntimeException e) {
                throw QqOpenApiErrorMapper.network(e);
            }
        }
        throw QqOpenApiErrorMapper.network(new IllegalStateException("QQ OpenAPI 重试未完成"));
    }

    private synchronized void refreshToken() {
        if (accessToken != null && needGetNewTokenTime != null
                && LocalDateTime.now().isBefore(needGetNewTokenTime)) {
            return;
        }
        Map<String, String> headers = Map.of("Content-Type", "application/json");
        Map<String, Object> request = Map.of(
                "appId", txBotProperty.getAppId(),
                "clientSecret", txBotProperty.getAppSecret()
        );
        try {
            AccessTokenDto token = requestAccessToken(request, headers);
            if (token == null || token.getAccessToken() == null || token.getAccessToken().isBlank()
                    || token.getExpiresIn() == null) {
                throw new IllegalStateException("QQ token 接口未返回有效凭证");
            }
            long refreshAfterSeconds = Math.max(1, token.getExpiresIn() - TOKEN_EXPIRY_BUFFER_SECONDS);
            accessToken = token.getAccessToken();
            needGetNewTokenTime = LocalDateTime.now().plusSeconds(refreshAfterSeconds);
        } catch (RemoteHttpException e) {
            clearToken();
            QqOpenApiException mapped = QqOpenApiErrorMapper.fromHttp(e, objectMapper);
            log.error("获取 QQ 调用凭证失败，invocationId=>{}，category=>{}，httpStatus=>{}，apiCode=>{}，qqTraceId=>{}",
                    RequestTraceContext.currentId().orElse(null), mapped.getCategory(),
                    mapped.getHttpStatus(), mapped.getApiCode(), mapped.getTraceId());
            throw mapped;
        } catch (RuntimeException e) {
            clearToken();
            QqOpenApiException mapped = e instanceof QqOpenApiException qqFailure
                    ? qqFailure : QqOpenApiErrorMapper.network(e);
            log.error("获取 QQ 调用凭证失败，invocationId=>{}，category=>{}，reason=>{}",
                    RequestTraceContext.currentId().orElse(null), mapped.getCategory(),
                    SensitiveDataUtil.summarize(e));
            throw mapped;
        }
    }

    private Map<String, String> authorizationHeader() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "QQBot " + accessToken);
        return headers;
    }

    private synchronized void invalidateToken() {
        clearToken();
    }

    private void clearToken() {
        accessToken = null;
        needGetNewTokenTime = null;
    }

    private Duration requestTimeout() {
        return Duration.ofSeconds(txBotProperty.getRequestTimeoutSeconds());
    }

    @FunctionalInterface
    private interface RequestCall<T> {
        T execute();
    }
}
