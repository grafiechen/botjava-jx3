package com.grafie.botjava.util;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

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
     * @param baseUrl    基础地址
     * @param url        请求url
     * @param param      请求参数
     * @param headersMap header
     * @return 接口返回的string字符串，根据接口内容
     */
    public static <T> T doPost(String baseUrl, String url, Map<String, Object> param, Map<String, String> headersMap, Class<T> clazz) {
        WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();
        try {
            return webClient.method(HttpMethod.POST)
                    .uri(uriBuilder -> uriBuilder.path(url).build())
                    .headers(headers -> headersMap.forEach(headers::add))
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(param)
                    .retrieve()
                    .bodyToMono(clazz)
                    .block(); // 同步等待响应
        } catch (WebClientResponseException e) {
            // 处理服务器返回的异常 (4xx 或 5xx 响应)
            System.err.println("HTTP Status: " + e.getStatusCode());
            System.err.println("Response Body: " + e.getResponseBodyAsString());
            throw new RuntimeException("Request failed with status: " + e.getStatusCode(), e);
        } catch (Exception e) {
            // 处理其他异常
            System.err.println("An error occurred: " + e.getMessage());
            throw new RuntimeException("Request failed", e);
        }
    }


}
