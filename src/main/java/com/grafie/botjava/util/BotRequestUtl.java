package com.grafie.botjava.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.config.TxBotProperty;
import com.grafie.botjava.entity.dto.TxFileUploadResultDto;
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
    public static final String fileUploadUrl = "/v2/groups/%s/files";

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
        requestParamMap.put("clientSecret", txBotProperty.getAppSecret());
        // 获取新的token
        try {
            AccessTokenDto accessTokenDto = RequestUtil.doPost(txBotProperty.getAccessTokenUrl(), null, requestParamMap, headerMap, AccessTokenDto.class);
            needGetNewTokenTime = LocalDateTime.now().plusSeconds(accessTokenDto.getExpiresIn());
            accessToken = accessTokenDto.getAccessToken();
        } catch (Exception e) {
            log.error("获取调用凭证接口出错，url=>{}，headerMap=>{}，requestParamMap=>{}",txBotProperty.getAccessTokenUrl(), headerMap, requestParamMap, e);
        }

    }

    public <T> T doPost(String path, Map<String, Object> param, Class<T> clazz) {
        refreshToken();
        Map<String, String> header = new HashMap<>();
        header.put("Authorization", "QQBot " + accessToken);
        // 暂时不关心返回值，先不管他
        return RequestUtil.doPost(txBotProperty.getOpenapiUrl(), path, param, header, clazz);
    }

    /**
     * 发送文件
     *
     * @param fileBaseUrl 原始地址
     * @param fileType    媒体类型：1 图片，2 视频，3 语音，4 文件（暂不开放）
     * @param requestUrl  请求地址
     *                    资源格式要求
     *                    图片：png/jpg，视频：mp4，语音：silk
     * @return TxFileUploadResultDto
     */
    public TxFileUploadResultDto doPostForUploadFile(String fileBaseUrl, String requestUrl, int fileType) {
        refreshToken();
        Map<String, String> header = new HashMap<>();
        header.put("Authorization", "QQBot " + accessToken);
        Map<String, Object> uploadFileRequest = new HashMap<>();
        uploadFileRequest.put("file_type", fileType);
        uploadFileRequest.put("url", fileBaseUrl);
        uploadFileRequest.put("srv_send_msg", false);
        // 暂时不关心返回值，先不管他
        return RequestUtil.doPost(txBotProperty.getOpenapiUrl(), requestUrl, uploadFileRequest, header, TxFileUploadResultDto.class);
    }
}
