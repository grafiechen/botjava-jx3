package com.grafie.botjava.service;

import com.grafie.botjava.entity.dto.TxFileUploadResultDto;
import com.grafie.botjava.entity.dto.common.MediaDto;
import com.grafie.botjava.entity.dto.common.TxMessageInfo;
import com.grafie.botjava.entity.dto.qq.QqGroupMessageSendResultDto;
import com.grafie.botjava.entity.dto.qq.QqMediaUploadPartFinishRequestDto;
import com.grafie.botjava.entity.dto.qq.QqMediaUploadPrepareRequestDto;
import com.grafie.botjava.entity.dto.qq.QqMediaUploadPrepareResultDto;
import com.grafie.botjava.entity.dto.qq.QqStreamMessageRequestDto;
import com.grafie.botjava.entity.dto.qq.QqStreamMessageResultDto;
import com.grafie.botjava.observability.BotMetrics;
import com.grafie.botjava.qq.QqOpenApiClient;
import com.grafie.botjava.qq.QqOpenApiErrorMapper;
import com.grafie.botjava.util.ObjectMapperUtil;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@SuppressWarnings("unchecked")
public class QqUserMessageClient {

    private static final String USER_MESSAGE_PATH = "/v2/users/%s/messages";
    private static final String USER_STREAM_MESSAGE_PATH = "/v2/users/%s/stream_messages";
    private static final String USER_FILE_PATH = "/v2/users/%s/files";
    private static final String USER_UPLOAD_PREPARE_PATH = "/v2/users/%s/upload_prepare";
    private static final String USER_UPLOAD_PART_FINISH_PATH = "/v2/users/%s/upload_part_finish";
    private static final int IMAGE_FILE_TYPE = 1;
    private static final int AUDIO_FILE_TYPE = 3;

    private final QqOpenApiClient openApiClient;
    private final BotMetrics botMetrics;

    public QqUserMessageClient(QqOpenApiClient openApiClient, BotMetrics botMetrics) {
        this.openApiClient = openApiClient;
        this.botMetrics = botMetrics;
    }

    public QqGroupMessageSendResultDto send(String userOpenId, TxMessageInfo messageInfo) {
        long startNanos = System.nanoTime();
        String outcome = "exception";
        try {
            Map<String, Object> requestBody = ObjectMapperUtil.getObjectMapper().convertValue(messageInfo, Map.class);
            QqGroupMessageSendResultDto result = openApiClient.post(
                    String.format(USER_MESSAGE_PATH, safePathSegment(userOpenId, "QQ user OpenID")),
                    requestBody,
                    QqGroupMessageSendResultDto.class
            );
            outcome = "success";
            return result;
        } finally {
            botMetrics.recordQqRequest("send_user_message", outcome, System.nanoTime() - startNanos);
        }
    }

    public QqStreamMessageResultDto stream(String userOpenId, QqStreamMessageRequestDto request) {
        Map<String, Object> requestBody = requestBody(request, "QQ user stream message request must not be null");
        return openApiClient.post(
                String.format(USER_STREAM_MESSAGE_PATH, safePathSegment(userOpenId, "QQ user OpenID")),
                requestBody,
                QqStreamMessageResultDto.class
        );
    }

    public void recallMessage(String userOpenId, String messageId) {
        long startNanos = System.nanoTime();
        String outcome = "exception";
        try {
            openApiClient.delete(
                    String.format(USER_MESSAGE_PATH + "/%s",
                            safePathSegment(userOpenId, "QQ user OpenID"),
                            safePathSegment(messageId, "QQ user message ID")),
                    Map.of(),
                    String.class
            );
            outcome = "success";
        } finally {
            botMetrics.recordQqRequest("recall_user_message", outcome, System.nanoTime() - startNanos);
        }
    }

    public MediaDto uploadImage(String userOpenId, String imageUrl) {
        return uploadMedia(userOpenId, imageUrl, IMAGE_FILE_TYPE, "upload_user_image");
    }

    public MediaDto uploadAudio(String userOpenId, String audioUrl) {
        return uploadMedia(userOpenId, audioUrl, AUDIO_FILE_TYPE, "upload_user_audio");
    }

    public TxFileUploadResultDto uploadFile(String userOpenId, Map<String, Object> request) {
        if (request == null) {
            throw new IllegalArgumentException("QQ user file upload request must not be null");
        }
        return openApiClient.post(
                String.format(USER_FILE_PATH, safePathSegment(userOpenId, "QQ user OpenID")),
                request,
                TxFileUploadResultDto.class
        );
    }

    public QqMediaUploadPrepareResultDto prepareUpload(String userOpenId, QqMediaUploadPrepareRequestDto request) {
        Map<String, Object> requestBody = requestBody(request, "QQ user media upload prepare request must not be null");
        return openApiClient.post(
                String.format(USER_UPLOAD_PREPARE_PATH, safePathSegment(userOpenId, "QQ user OpenID")),
                requestBody,
                QqMediaUploadPrepareResultDto.class
        );
    }

    public void finishUploadPart(String userOpenId, QqMediaUploadPartFinishRequestDto request) {
        Map<String, Object> requestBody = requestBody(request, "QQ user media upload part finish request must not be null");
        openApiClient.post(
                String.format(USER_UPLOAD_PART_FINISH_PATH, safePathSegment(userOpenId, "QQ user OpenID")),
                requestBody,
                String.class
        );
    }

    private MediaDto uploadMedia(String userOpenId, String mediaUrl, int fileType, String operation) {
        long startNanos = System.nanoTime();
        String outcome = "exception";
        try {
            TxFileUploadResultDto uploadResult = uploadFile(userOpenId, Map.of(
                    "file_type", fileType,
                    "url", mediaUrl,
                    "srv_send_msg", false
            ));
            if (uploadResult == null || uploadResult.getFileInfo() == null || uploadResult.getFileInfo().isBlank()) {
                outcome = "invalid_response";
                throw QqOpenApiErrorMapper.invalidResponse("QQ user media upload");
            }
            outcome = "success";
            return new MediaDto(uploadResult.getFileInfo());
        } finally {
            botMetrics.recordQqRequest(operation, outcome, System.nanoTime() - startNanos);
        }
    }

    private Map<String, Object> requestBody(Object request, String message) {
        if (request == null) {
            throw new IllegalArgumentException(message);
        }
        return ObjectMapperUtil.getObjectMapper().convertValue(request, Map.class);
    }

    private String safePathSegment(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        String trimmed = value.trim();
        if (trimmed.contains("/")) {
            throw new IllegalArgumentException(name + " must not contain path separator");
        }
        return trimmed;
    }
}