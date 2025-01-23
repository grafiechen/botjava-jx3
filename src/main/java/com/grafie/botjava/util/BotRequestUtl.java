package com.grafie.botjava.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.config.TxBotProperty;
import com.grafie.botjava.entity.dto.token.AccessTokenDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * @author grafie.chen
 * @since 2025/1/23  9:30
 */
@Slf4j
@Component
public class BotRequestUtl {

    /**
     * 上次获取token的时间
     * 与当前时间差距60秒以内时，才会获取一个新的token
     * 逻辑：当当前时间比这个时间大时，请求刷新token
     */
    private LocalDateTime needGetNewTokenTime;
    private final TxBotProperty txBotProperty;
    private final ObjectMapper objectMapper;
    /**
     * 通过接口获取到的token值
     */
    private String accessToken;

    public BotRequestUtl(TxBotProperty txBotProperty, ObjectMapper objectMapper) {
        this.txBotProperty = txBotProperty;
        this.objectMapper = objectMapper;
    }

    public void refreshToken() {
        if (needGetNewTokenTime != null && LocalDateTime.now().isBefore(needGetNewTokenTime)) {
            return;
        }
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("Content-Type", "application/json");
        Map<String, Object> requestParamMap = new HashMap<>();
        requestParamMap.put("appId", txBotProperty.getAppId());
        requestParamMap.put("clientSecret", txBotProperty.getClientSecret());
        // 获取新的token
        try {
            String result = RequestUtil.doPost(txBotProperty.getSignBaseUrl(),txBotProperty.getSignUrl(), requestParamMap, headerMap);
            AccessTokenDto accessTokenDto = objectMapper.readValue(result, AccessTokenDto.class);
            needGetNewTokenTime = LocalDateTime.now().plusSeconds(accessTokenDto.getExpiresIn());
            accessToken = accessTokenDto.getAccessToken();
        } catch (Exception e) {
            log.error("获取调用凭证接口出错，headerMap=>{}，requestParamMap=>{}", headerMap, requestParamMap, e);
        }

    }

    public String doPost(String path, Map<String, Object> param) {
        refreshToken();
        Map<String, String> header = new HashMap<>();
        header.put("Authorization", accessToken);
        return RequestUtil.doPost(txBotProperty.getServer(),path, param, header);
    }
}
