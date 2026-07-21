package com.grafie.botjava.jx3.http.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.client.WebClient;
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
@Slf4j
public class Jx3RequestUtil {
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
        this.webClient = WebClient.builder().baseUrl(apiProperties.getApiUrl()).defaultHeader("token", apiProperties.getApiToken()).defaultHeader(HttpHeaders.USER_AGENT, "Nonebot2-jx3-bot").build();
        this.objectMapper = objectMapper;
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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
    public RequestResult doPostRequest(String path, Map<String, Object> params) {
        long startNanos = System.nanoTime();
        Map<String, Object> requestParams = params == null ? new HashMap<>() : new HashMap<>(params);
        requestParams.put("token", apiProperties.getApiToken());
        boolean cacheable = responseCache.isCacheable(path);
        java.util.Optional<RequestResult> cached = responseCache.get(path, requestParams);
        if (cached.isPresent()) {
            botMetrics.recordJx3Cache(path, "hit");
            log.info("命中 JX3API 缓存，path=>{}", path);
            botMetrics.recordJx3Request(path, "cache", outcome(cached.get()), System.nanoTime() - startNanos);
            return cached.get();
        }
        if (cacheable) {
            botMetrics.recordJx3Cache(path, "miss");
        }
        log.info("请求 JX3API，path=>{}", path);
        String outcome = "exception";
        try {
            Mono<RequestResult> mono = this.webClient.method(HttpMethod.POST)
                    .uri(uriBuilder -> uriBuilder.path(path).build())
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON).bodyValue(requestParams).retrieve().bodyToMono(RequestResult.class);
            RequestResult result = mono.block();
            outcome = outcome(result);
            responseCache.put(path, requestParams, result);
            return result;
        } finally {
            botMetrics.recordJx3Request(path, "remote", outcome, System.nanoTime() - startNanos);
        }
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
            } catch (JsonProcessingException e) {
                log.error("反序列化 JX3API 列表响应失败，method=>{}，reason=>{}",
                        methodEnum.name(), com.grafie.botjava.util.SensitiveDataUtil.summarize(e));
                // 不想给调用方加thr了，换个runtime抛出去把
                throw new RuntimeException("序列化参数时，出现异常");
            }
        } else {
            if (methodEnum.getJsonKey() == null) {
                baseResult.setData(objectMapper.convertValue(requestResult.getData(), methodEnum.getResultBeanClass()));
            } else {
                baseResult.setData(objectMapper.convertValue(((Map<String, Object>) requestResult.getData()).get(methodEnum.getJsonKey()), methodEnum.getResultBeanClass()));
            }
        }
        return baseResult;
    }
}
