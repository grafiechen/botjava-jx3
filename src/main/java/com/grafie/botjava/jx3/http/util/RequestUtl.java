package com.grafie.botjava.jx3.http.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.MethodEnum;
import com.grafie.botjava.jx3.http.RequestResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * @author jinpeng.chen
 * @since 2025/1/22  15:36
 */
@Slf4j
public class RequestUtl {
    private final WebClient webClient;
    /**
     * 相关api参数
     */
    private ApiProperties apiProperties;

    private ObjectMapper objectMapper;

    public RequestUtl(ApiProperties apiProperties, ObjectMapper objectMapper) {
        this.apiProperties = apiProperties;
        this.webClient = WebClient.builder().baseUrl(apiProperties.getApiUrl()).defaultHeader("token", apiProperties.getApiToken()).defaultHeader(HttpHeaders.USER_AGENT, "Nonebot2-jx3-bot").build();
        this.objectMapper = objectMapper;
    }

    /**
     * 执行post请求
     *
     * @param path   请求地址
     * @param params 使用的参数
     * @return 返回内容
     */
    public RequestResult doPostRequest(String path, Map<String, Object> params) {
        params.put("token", apiProperties.getApiToken());
        log.info("请求接口=>{},参数=>{}", path, params);
        Mono<RequestResult> mono = this.webClient.method(HttpMethod.POST)
                .uri(uriBuilder -> uriBuilder.path(path).build())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).bodyValue(params).retrieve().bodyToMono(RequestResult.class);
        return mono.block();
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
            log.error("返回值不为空，请求名称=>{},请求地址=>{},返回值信息=>{}", methodEnum.getMethodName(), methodEnum.getMethodPath(), requestResult);
            BaseResult baseResult = new BaseResult();
            baseResult.setCode(500);
            baseResult.setMsg("服务器返回值为空");
            return baseResult;
        }
        if (HttpStatus.OK.value() != requestResult.getCode()) {
            log.error("返回值不成功，请求名称=>{},请求地址=>{},返回值信息=>{}", methodEnum.getMethodName(), methodEnum.getMethodPath(), requestResult);
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
                    result = objectMapper.readValue(((Map<String, String>) requestResult.getData()).get(methodEnum.getJsonKey()), listType);
                }
                baseResult.setData(result);
            } catch (JsonProcessingException e) {
                log.error("序列化参数时，出现异常，请求参数=>{}", requestResult.getData(), e);
                // 不想给调用方加thr了，换个runtime抛出去把
                throw new RuntimeException("序列化参数时，出现异常");
            }
        } else {
            if (methodEnum.getJsonKey() == null) {
                baseResult.setData(objectMapper.convertValue(requestResult.getData(), methodEnum.getResultBeanClass()));
            } else {
                baseResult.setData(objectMapper.convertValue(((Map<String, String>) requestResult.getData()).get(methodEnum.getJsonKey()), methodEnum.getResultBeanClass()));
            }
        }
        return baseResult;
    }
}
