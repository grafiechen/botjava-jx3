package com.grafie.botjava.jx3.http.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.MethodEnum;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.cache.Jx3ApiResponseCache;
import com.grafie.botjava.observability.BotMetrics;
import com.grafie.botjava.util.SensitiveDataUtil;
import com.grafie.botjava.util.RemoteImageDataUriLoader;
import com.grafie.botjava.util.OutboundHttpRateLimiter;
import com.grafie.botjava.util.ObjectMapperUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author grafie.chen
 * @since 2025/1/22  15:36
 */
@Component
@ConditionalOnProperty(prefix = "jx3api", name = {"enabled", "http.enabled"}, havingValue = "true", matchIfMissing = true)
@Slf4j
public class Jx3RequestUtil {
    private static final int LOG_BODY_CHUNK_SIZE = 3000;
    private final WebClient webClient;
    /**
     * 相关api参数
     */
    private ApiProperties apiProperties;

    private ObjectMapper objectMapper;
    private final Jx3ApiResponseCache responseCache;
    private final BotMetrics botMetrics;
    private final RemoteImageDataUriLoader remoteImageDataUriLoader;

    @Autowired
    public Jx3RequestUtil(ApiProperties apiProperties, ObjectMapper objectMapper,
                          Jx3ApiResponseCache responseCache, BotMetrics botMetrics,
                          RemoteImageDataUriLoader remoteImageDataUriLoader) {
        this(apiProperties, objectMapper, responseCache, botMetrics, remoteImageDataUriLoader, true);
    }

    public Jx3RequestUtil(ApiProperties apiProperties, ObjectMapper objectMapper,
                          Jx3ApiResponseCache responseCache, BotMetrics botMetrics) {
        this(apiProperties, objectMapper, responseCache, botMetrics, null, true);
    }

    private Jx3RequestUtil(ApiProperties apiProperties, ObjectMapper objectMapper,
                           Jx3ApiResponseCache responseCache, BotMetrics botMetrics,
                           RemoteImageDataUriLoader remoteImageDataUriLoader, boolean ignored) {
        this.apiProperties = apiProperties;
        this.responseCache = responseCache;
        this.botMetrics = botMetrics;
        this.remoteImageDataUriLoader = remoteImageDataUriLoader;
        this.webClient = WebClient.builder().baseUrl(apiProperties.getApiUrl()).defaultHeader(HttpHeaders.USER_AGENT, "Nonebot2-jx3-bot").build();
        this.objectMapper = ObjectMapperUtil.configure(objectMapper);
    }

    public Optional<String> loadRemoteImageDataUri(String url) {
        return remoteImageDataUriLoader == null ? Optional.empty() : remoteImageDataUriLoader.load(url);
    }

    /**
     * 执行post请求
     *
     * @param path   请求地址
     * @param params 使用的参数
     * @return 返回内容
     */
    public RequestResult doPostRequest(MethodEnum methodEnum, Map<String, Object> params) {
        if (methodEnum == null) {
            throw new IllegalArgumentException("methodEnum can not be null");
        }
        return doRequest(methodEnum.getMethodPath(), params,
                apiProperties.resolveToken(methodEnum.getApiLevel()), HttpMethod.POST);
    }

    public RequestResult doGetRequest(MethodEnum methodEnum, Map<String, Object> params) {
        if (methodEnum == null) {
            throw new IllegalArgumentException("methodEnum can not be null");
        }
        return doRequest(methodEnum.getMethodPath(), params,
                apiProperties.resolveToken(methodEnum.getApiLevel()), HttpMethod.GET);
    }

    public RequestResult doPostRequest(String path, Map<String, Object> params) {
        MethodEnum methodEnum = MethodEnum.findByPath(path);
        int apiLevel = methodEnum == null ? 1 : methodEnum.getApiLevel();
        return doRequest(path, params, apiProperties.resolveToken(apiLevel), HttpMethod.POST);
    }

    private RequestResult doRequest(String path, Map<String, Object> params,
                                    String requestToken, HttpMethod httpMethod) {
        long startNanos = System.nanoTime();
        Map<String, Object> requestParams = params == null ? new HashMap<>() : new HashMap<>(params);
        requestParams.put("token", requestToken);
        boolean cacheable = responseCache.isCacheable(path);
        java.util.Optional<RequestResult> cached = responseCache.get(path, requestParams);
        if (cached.isPresent()) {
            botMetrics.recordJx3Cache(path, "hit");
            log.info("命中 JX3API 缓存，method=>{}，path=>{}", httpMethod, path);
            botMetrics.recordJx3Request(path, "cache", outcome(cached.get()), System.nanoTime() - startNanos);
            return cached.get();
        }
        if (cacheable) {
            botMetrics.recordJx3Cache(path, "miss");
        }
        OutboundHttpRateLimiter.awaitPermit();
        log.info("外部 HTTP 请求开始，service=>JX3API，method=>{}，baseUrl=>{}，path=>{}，{}=>{}",
                httpMethod, apiProperties.getApiUrl(), path,
                HttpMethod.GET.equals(httpMethod) ? "query" : "body", SensitiveDataUtil.redact(requestParams));
        String outcome = "exception";
        try {
            Mono<String> mono;
            if (HttpMethod.GET.equals(httpMethod)) {
                mono = this.webClient.method(HttpMethod.GET)
                        .uri(uriBuilder -> {
                            var builder = uriBuilder.path(path);
                            requestParams.forEach(builder::queryParam);
                            return builder.build();
                        })
                        .headers(headers -> headers.set("token", requestToken))
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .bodyToMono(String.class);
            } else {
                mono = this.webClient.method(httpMethod)
                        .uri(uriBuilder -> uriBuilder.path(path).build())
                        .headers(headers -> headers.set("token", requestToken))
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(requestParams)
                        .retrieve()
                        .bodyToMono(String.class);
            }
            String responseBody = mono.block();
            logResponseBody(httpMethod, path, startNanos, responseBody);
            RequestResult result = parseRequestResult(path, responseBody);
            outcome = outcome(result);
            log.info("外部 HTTP 请求完成，service=>JX3API，method=>{}，baseUrl=>{}，path=>{}，elapsedMs=>{}，code=>{}，message=>{}",
                    httpMethod, apiProperties.getApiUrl(), path, elapsedMillis(startNanos),
                    result == null ? null : result.getCode(),
                    result == null ? null : SensitiveDataUtil.redactText(result.getMsg()));
            responseCache.put(path, requestParams, result);
            return result;
        } catch (WebClientResponseException e) {
            log.error("外部 HTTP 请求返回错误，service=>JX3API，method=>{}，baseUrl=>{}，path=>{}，{}=>{}，status=>{}，elapsedMs=>{}，responseBody=>{}",
                    httpMethod, apiProperties.getApiUrl(), path,
                    HttpMethod.GET.equals(httpMethod) ? "query" : "body", SensitiveDataUtil.redact(requestParams),
                    e.getStatusCode().value(), elapsedMillis(startNanos),
                    SensitiveDataUtil.redactText(e.getResponseBodyAsString()), e);
            throw e;
        } catch (RuntimeException e) {
            log.error("外部 HTTP 请求失败，service=>JX3API，method=>{}，baseUrl=>{}，path=>{}，{}=>{}，elapsedMs=>{}，reason=>{}",
                    httpMethod, apiProperties.getApiUrl(), path,
                    HttpMethod.GET.equals(httpMethod) ? "query" : "body", SensitiveDataUtil.redact(requestParams),
                    elapsedMillis(startNanos), SensitiveDataUtil.summarize(e), e);
            throw e;
        } finally {
            botMetrics.recordJx3Request(path, "remote", outcome, System.nanoTime() - startNanos);
        }
    }

    private RequestResult parseRequestResult(String path, String responseBody) {
        try {
            RequestResult result = objectMapper.readValue(responseBody, RequestResult.class);
            if (result == null) {
                throw new IllegalArgumentException("JX3API 响应体为空");
            }
            result.setRawResponseBody(responseBody);
            return result;
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.error("解析 JX3API 原始响应失败，path=>{}，responseBody=>{}，reason=>{}",
                    path, logBody(responseBody), SensitiveDataUtil.summarize(e), e);
            throw new RuntimeException("解析 JX3API 响应失败", e);
        }
    }

    private void logResponseBody(HttpMethod httpMethod, String path, long startNanos, String responseBody) {
        String body = logBody(responseBody);
        if (body == null || body.length() <= LOG_BODY_CHUNK_SIZE) {
            log.info("外部 HTTP 请求收到响应，service=>JX3API，method=>{}，baseUrl=>{}，path=>{}，elapsedMs=>{}，responseBody=>{}",
                    httpMethod, apiProperties.getApiUrl(), path, elapsedMillis(startNanos), body);
            return;
        }
        int totalParts = (body.length() + LOG_BODY_CHUNK_SIZE - 1) / LOG_BODY_CHUNK_SIZE;
        log.info("外部 HTTP 请求收到响应，service=>JX3API，method=>{}，baseUrl=>{}，path=>{}，elapsedMs=>{}，responseBodyLength=>{}，responseParts=>{}",
                httpMethod, apiProperties.getApiUrl(), path, elapsedMillis(startNanos), body.length(), totalParts);
        for (int part = 0; part < totalParts; part++) {
            int start = part * LOG_BODY_CHUNK_SIZE;
            int end = Math.min(body.length(), start + LOG_BODY_CHUNK_SIZE);
            log.info("外部 HTTP 请求响应内容，service=>JX3API，method=>{}，baseUrl=>{}，path=>{}，part=>{}/{}，responseBody=>{}",
                    httpMethod, apiProperties.getApiUrl(), path, part + 1, totalParts, body.substring(start, end));
        }
    }

    private String logBody(String body) {
        if (body == null) {
            return null;
        }
        return SensitiveDataUtil.redactText(body.replace('\r', ' ').replace('\n', ' ').trim());
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private String outcome(RequestResult result) {
        if (result == null) {
            return "empty";
        }
        return result.getCode() != null && HttpStatus.OK.value() == result.getCode()
                ? "success" : "api_error";
    }

    /**
     * 获取序列化后的返回值
     *
     * @param requestResult 返回值信息
     * @param methodEnum    请求枚举
     * @return 序列化后的返回值，根据 MethodEnum.resultBeanClass 进行序列化
     */
    public <T> BaseResult<T> getResultRealData(RequestResult requestResult, MethodEnum methodEnum) {
        if (requestResult == null) {
            log.error("JX3API 返回值为空，请求名称=>{},请求地址=>{}", methodEnum.getMethodName(), methodEnum.getMethodPath());
            BaseResult baseResult = new BaseResult();
            baseResult.setCode(500);
            baseResult.setMsg("服务器返回值为空");
            return baseResult;
        }
        if (HttpStatus.OK.value() != requestResult.getCode()) {
            log.error("JX3API 返回值不成功，请求名称=>{}，请求地址=>{}，code=>{}，message=>{}",
                    methodEnum.getMethodName(), methodEnum.getMethodPath(), requestResult.getCode(),
                    SensitiveDataUtil.redactText(requestResult.getMsg()));
            BaseResult baseResult = new BaseResult();
            baseResult.setCode(requestResult.getCode());
            baseResult.setMsg(requestResult.getMsg());
            return baseResult;
        }
        BaseResult baseResult = new BaseResult();
        baseResult.setCode(requestResult.getCode());
        baseResult.setMsg(requestResult.getMsg());
        baseResult.setTime(requestResult.getTime());
        // 根据枚举优先判断pClass类型，区分主体对象是不是List
        if (List.class.isAssignableFrom(methodEnum.getpClass())) {
            TypeFactory typeFactory = objectMapper.getTypeFactory();
            CollectionType listType = typeFactory.constructCollectionType(List.class, methodEnum.getResultBeanClass());
            try {
                List<T> result = null;
                if (methodEnum.getJsonKey() == null) {
                    result = objectMapper.readValue(objectMapper.writeValueAsString(requestResult.getData()), listType);
                } else {
                    result = objectMapper.readValue(objectMapper.writeValueAsString(((Map<String, Object>) requestResult.getData()).get(methodEnum.getJsonKey())), listType);
                }
                baseResult.setData(result);
            } catch (JsonProcessingException | IllegalArgumentException e) {
                log.error("反序列化 JX3API 列表响应失败，method=>{}，responseBody=>{}，reason=>{}",
                        methodEnum.name(), logBody(requestResult.getRawResponseBody()), SensitiveDataUtil.summarize(e), e);
                throw new RuntimeException("序列化参数时，出现异常", e);
            }
        } else {
            try {
                if (methodEnum.getJsonKey() == null) {
                    baseResult.setData(objectMapper.convertValue(requestResult.getData(), methodEnum.getResultBeanClass()));
                } else {
                    baseResult.setData(objectMapper.convertValue(((Map<String, Object>) requestResult.getData()).get(methodEnum.getJsonKey()), methodEnum.getResultBeanClass()));
                }
            } catch (IllegalArgumentException e) {
                log.error("反序列化 JX3API 对象响应失败，method=>{}，responseBody=>{}，reason=>{}",
                        methodEnum.name(), logBody(requestResult.getRawResponseBody()), SensitiveDataUtil.summarize(e), e);
                throw new RuntimeException("序列化参数时，出现异常", e);
            }
        }
        return baseResult;
    }
}
