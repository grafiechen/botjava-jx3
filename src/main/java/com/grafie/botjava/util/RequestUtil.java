package com.grafie.botjava.util;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * @author grafie.chen
 * @since 2025/1/23  11:22
 */
public class RequestUtil {

    /**
     * 执行post请求。默认 Content-Type 为 application/json。
     * header只允许是String
     *
     * @param baseUrl 基础地址
     * @param url    请求url
     * @param param      请求参数
     * @param headersMap header
     * @return 接口返回的string字符串，根据接口内容
     */
    public static String doPost(String baseUrl,String url, Map<String, Object> param, Map<String, String> headersMap) {
        WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();
        Mono<String> mono = webClient.method(HttpMethod.POST)
                .uri(uriBuilder -> uriBuilder.path(url).build())
                .headers(headers -> headersMap.forEach((key, value) -> headers.add(key, String.valueOf(value))))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).bodyValue(param).retrieve().bodyToMono(String.class);
        return mono.block();
    }
}
