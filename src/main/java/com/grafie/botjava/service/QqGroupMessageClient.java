package com.grafie.botjava.service;

import com.grafie.botjava.entity.dto.TxFileUploadResultDto;
import com.grafie.botjava.entity.dto.common.MediaDto;
import com.grafie.botjava.entity.dto.common.TxMessageInfo;
import com.grafie.botjava.util.ObjectMapperUtil;
import com.grafie.botjava.qq.QqOpenApiClient;
import com.grafie.botjava.qq.QqOpenApiErrorMapper;
import com.grafie.botjava.observability.BotMetrics;
import com.grafie.botjava.observability.RequestTraceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * QQ 群消息 OpenAPI 客户端。
 * <p>
 * 统一管理群消息发送和群文件上传路径，业务发送器不直接拼接 QQ 接口地址。
 */
@Slf4j
@Service
@SuppressWarnings("unchecked")
public class QqGroupMessageClient {

    private static final String GROUP_MESSAGE_PATH = "/v2/groups/%s/messages";
    private static final String GROUP_FILE_PATH = "/v2/groups/%s/files";
    private static final int IMAGE_FILE_TYPE = 1;
    private static final int AUDIO_FILE_TYPE = 3;

    private final QqOpenApiClient openApiClient;
    private final BotMetrics botMetrics;

    public QqGroupMessageClient(QqOpenApiClient openApiClient, BotMetrics botMetrics) {
        this.openApiClient = openApiClient;
        this.botMetrics = botMetrics;
    }

    public Object send(String groupOpenId, TxMessageInfo messageInfo) {
        long startNanos = System.nanoTime();
        String outcome = "exception";
        try {
            Map<String, Object> requestBody = ObjectMapperUtil.getObjectMapper().convertValue(messageInfo, Map.class);
            Object result = openApiClient.post(
                    String.format(GROUP_MESSAGE_PATH, groupOpenId),
                    requestBody,
                    String.class
            );
            outcome = "success";
            log.info("发送群消息完成，invocationId=>{}，resultType=>{}",
                    RequestTraceContext.currentId().orElse(null),
                    result == null ? null : result.getClass().getSimpleName());
            return result;
        } finally {
            botMetrics.recordQqRequest("send_message", outcome, System.nanoTime() - startNanos);
        }
    }

    public MediaDto uploadImage(String groupOpenId, String imageUrl) {
        return uploadMedia(groupOpenId, imageUrl, IMAGE_FILE_TYPE, "图片", "upload_image");
    }

    public MediaDto uploadAudio(String groupOpenId, String audioUrl) {
        return uploadMedia(groupOpenId, audioUrl, AUDIO_FILE_TYPE, "语音", "upload_audio");
    }

    private MediaDto uploadMedia(String groupOpenId, String mediaUrl, int fileType,
                                 String mediaName, String operation) {
        long startNanos = System.nanoTime();
        String outcome = "exception";
        try {
            Map<String, Object> request = Map.of(
                    "file_type", fileType,
                    "url", mediaUrl,
                    "srv_send_msg", false
            );
            TxFileUploadResultDto uploadResult = openApiClient.post(
                    String.format(GROUP_FILE_PATH, groupOpenId), request, TxFileUploadResultDto.class);
            if (uploadResult == null || uploadResult.getFileInfo() == null || uploadResult.getFileInfo().isBlank()) {
                outcome = "invalid_response";
                throw QqOpenApiErrorMapper.invalidResponse("QQ " + mediaName + "上传");
            }
            outcome = "success";
            return new MediaDto(uploadResult.getFileInfo());
        } finally {
            botMetrics.recordQqRequest(operation, outcome, System.nanoTime() - startNanos);
        }
    }
}
