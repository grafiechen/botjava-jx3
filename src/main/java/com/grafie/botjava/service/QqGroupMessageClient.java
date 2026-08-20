package com.grafie.botjava.service;

import com.grafie.botjava.entity.dto.TxFileUploadResultDto;
import com.grafie.botjava.entity.dto.common.MediaDto;
import com.grafie.botjava.entity.dto.common.TxMessageInfo;
import com.grafie.botjava.entity.dto.qq.QqBotGroupStateDto;
import com.grafie.botjava.entity.dto.qq.QqGroupMessageSendResultDto;
import com.grafie.botjava.entity.dto.qq.QqGroupInfoDto;
import com.grafie.botjava.entity.dto.qq.QqGroupJoinApprovalRequestDto;
import com.grafie.botjava.entity.dto.qq.QqGroupJoinRequestListDto;
import com.grafie.botjava.entity.dto.qq.QqGroupMemberMuteRequestDto;
import com.grafie.botjava.entity.dto.qq.QqGroupRestrictChatSettingDto;
import com.grafie.botjava.entity.dto.qq.QqJoinApprovalStrategyListDto;
import com.grafie.botjava.entity.dto.qq.QqJoinApprovalStrategyRequestDto;
import com.grafie.botjava.entity.dto.qq.QqJoinApprovalStrategyResultDto;
import com.grafie.botjava.entity.dto.qq.QqJoinApprovalStrategyUpdateRequestDto;
import com.grafie.botjava.entity.dto.qq.QqJoinApprovalStrategyWhitelistRequestDto;
import com.grafie.botjava.entity.dto.qq.QqJoinApprovalStrategyWhitelistResultDto;
import com.grafie.botjava.entity.dto.qq.QqMediaUploadPartFinishRequestDto;
import com.grafie.botjava.entity.dto.qq.QqMediaUploadPrepareRequestDto;
import com.grafie.botjava.entity.dto.qq.QqMediaUploadPrepareResultDto;
import com.grafie.botjava.util.ObjectMapperUtil;
import com.grafie.botjava.util.OutboundHttpRateLimiter;
import com.grafie.botjava.qq.QqOpenApiClient;
import com.grafie.botjava.qq.QqOpenApiErrorMapper;
import com.grafie.botjava.observability.BotMetrics;
import com.grafie.botjava.observability.RequestTraceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
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
    private static final String GROUP_INFO_PATH = "/v2/groups/%s/info";
    private static final String GROUP_BOT_STATE_PATH = "/v2/groups/%s/bot_state";
    private static final String GROUP_JOIN_REQUEST_LIST_PATH = "/v2/groups/%s/join_request_list";
    private static final String GROUP_JOIN_APPROVAL_PATH = "/v2/groups/%s/approval_join_request/%s";
    private static final String GROUP_RESTRICT_CHAT_SETTING_PATH = "/v2/groups/%s/restrict_chat_setting";
    private static final String GROUP_JOIN_APPROVAL_STRATEGY_PATH = "/v2/groups/join_approval_strategy";
    private static final String GROUP_JOIN_APPROVAL_STRATEGY_DETAIL_PATH = "/v2/groups/join_approval_strategy/%s";
    private static final String GROUP_JOIN_APPROVAL_STRATEGY_EXECUTE_PATH = "/v2/groups/join_approval_strategy/%s/execute";
    private static final String GROUP_JOIN_APPROVAL_STRATEGY_WHITELIST_PATH = "/v2/groups/join_approval_strategy/%s/whitelist_users";    private static final String GROUP_UPLOAD_PREPARE_PATH = "/v2/groups/%s/upload_prepare";
    private static final String GROUP_UPLOAD_PART_FINISH_PATH = "/v2/groups/%s/upload_part_finish";
    private static final int IMAGE_FILE_TYPE = 1;
    private static final int AUDIO_FILE_TYPE = 3;
    private static final long MAX_MEDIA_BYTES = 200L * 1024 * 1024;
    private static final long MD5_TEN_MB_BYTES = 10_002_432L;
    private static final Duration PRESIGNED_UPLOAD_TIMEOUT = Duration.ofSeconds(60);

    private final QqOpenApiClient openApiClient;
    private final BotMetrics botMetrics;
    private final HttpClient httpClient;

    public QqGroupMessageClient(QqOpenApiClient openApiClient, BotMetrics botMetrics) {
        this.openApiClient = openApiClient;
        this.botMetrics = botMetrics;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public QqGroupInfoDto getGroupInfo(String groupOpenId) {
        return openApiClient.get(
                String.format(GROUP_INFO_PATH, safeGroupOpenId(groupOpenId)),
                Map.of(),
                QqGroupInfoDto.class
        );
    }

    public QqBotGroupStateDto getBotState(String groupOpenId) {
        return openApiClient.get(
                String.format(GROUP_BOT_STATE_PATH, safeGroupOpenId(groupOpenId)),
                Map.of(),
                QqBotGroupStateDto.class
        );
    }

    public QqGroupJoinRequestListDto listJoinRequests(String groupOpenId) {
        return listJoinRequests(groupOpenId, null, null);
    }

    public QqGroupJoinRequestListDto listJoinRequests(String groupOpenId, String cursor, Integer limit) {
        return openApiClient.get(
                String.format(GROUP_JOIN_REQUEST_LIST_PATH, safeGroupOpenId(groupOpenId)),
                pagingQuery(cursor, limit),
                QqGroupJoinRequestListDto.class
        );
    }

    public void approveJoinRequest(String groupOpenId, String memberOpenId,
                                   QqGroupJoinApprovalRequestDto request) {
        Map<String, Object> requestBody = requestBody(request, "QQ group join approval request must not be null");
        openApiClient.post(
                String.format(GROUP_JOIN_APPROVAL_PATH,
                        safeGroupOpenId(groupOpenId), safeMemberOpenId(memberOpenId)),
                requestBody,
                String.class
        );
    }

    public QqGroupRestrictChatSettingDto getRestrictChatSetting(String groupOpenId) {
        return openApiClient.get(
                String.format(GROUP_RESTRICT_CHAT_SETTING_PATH, safeGroupOpenId(groupOpenId)),
                Map.of(),
                QqGroupRestrictChatSettingDto.class
        );
    }

    public void updateRestrictChatSetting(String groupOpenId, QqGroupMemberMuteRequestDto request) {
        Map<String, Object> requestBody = requestBody(request, "QQ group mute request must not be null");
        openApiClient.post(
                String.format(GROUP_RESTRICT_CHAT_SETTING_PATH, safeGroupOpenId(groupOpenId)),
                requestBody,
                String.class
        );
    }

    public QqJoinApprovalStrategyListDto listJoinApprovalStrategies(String cursor, Integer limit) {
        return openApiClient.get(
                GROUP_JOIN_APPROVAL_STRATEGY_PATH,
                pagingQuery(cursor, limit),
                QqJoinApprovalStrategyListDto.class
        );
    }

    public QqJoinApprovalStrategyResultDto createJoinApprovalStrategy(QqJoinApprovalStrategyRequestDto request) {
        Map<String, Object> requestBody = requestBody(request, "QQ join approval strategy request must not be null");
        return openApiClient.post(
                GROUP_JOIN_APPROVAL_STRATEGY_PATH,
                requestBody,
                QqJoinApprovalStrategyResultDto.class
        );
    }

    public QqJoinApprovalStrategyResultDto updateJoinApprovalStrategy(String strategyId,
                                                                      QqJoinApprovalStrategyUpdateRequestDto request) {
        Map<String, Object> requestBody = requestBody(request, "QQ join approval strategy update request must not be null");
        return openApiClient.patch(
                String.format(GROUP_JOIN_APPROVAL_STRATEGY_DETAIL_PATH, safeStrategyId(strategyId)),
                requestBody,
                QqJoinApprovalStrategyResultDto.class
        );
    }

    public void deleteJoinApprovalStrategy(String strategyId) {
        openApiClient.delete(
                String.format(GROUP_JOIN_APPROVAL_STRATEGY_DETAIL_PATH, safeStrategyId(strategyId)),
                Map.of(),
                String.class
        );
    }

    public void executeJoinApprovalStrategy(String strategyId) {
        openApiClient.post(
                String.format(GROUP_JOIN_APPROVAL_STRATEGY_EXECUTE_PATH, safeStrategyId(strategyId)),
                Map.of(),
                String.class
        );
    }

    public QqJoinApprovalStrategyWhitelistResultDto updateJoinApprovalStrategyWhitelistUsers(
            String strategyId, QqJoinApprovalStrategyWhitelistRequestDto request) {
        Map<String, Object> requestBody = requestBody(request, "QQ join approval strategy whitelist request must not be null");
        return openApiClient.post(
                String.format(GROUP_JOIN_APPROVAL_STRATEGY_WHITELIST_PATH, safeStrategyId(strategyId)),
                requestBody,
                QqJoinApprovalStrategyWhitelistResultDto.class
        );
    }

    public QqGroupMessageSendResultDto send(String groupOpenId, TxMessageInfo messageInfo) {
        long startNanos = System.nanoTime();
        String outcome = "exception";
        try {
            Map<String, Object> requestBody = ObjectMapperUtil.getObjectMapper().convertValue(messageInfo, Map.class);
            QqGroupMessageSendResultDto result = openApiClient.post(
                    String.format(GROUP_MESSAGE_PATH, safeGroupOpenId(groupOpenId)),
                    requestBody,
                    QqGroupMessageSendResultDto.class
            );
            outcome = "success";
            log.info("发送群消息完成，invocationId=>{}，hasMsgId=>{}，hasRefIdx=>{}",
                    RequestTraceContext.currentId().orElse(null),
                    result != null && result.getId() != null && !result.getId().isBlank(),
                    result != null && result.getExtInfo() != null && result.getExtInfo().getRefIdx() != null);
            return result;
        } finally {
            botMetrics.recordQqRequest("send_message", outcome, System.nanoTime() - startNanos);
        }
    }

    public void recallMessage(String groupOpenId, String messageId) {
        long startNanos = System.nanoTime();
        String outcome = "exception";
        try {
            openApiClient.delete(
                    String.format(GROUP_MESSAGE_PATH + "/%s",
                            safeGroupOpenId(groupOpenId), safeMessageId(messageId)),
                    Map.of(),
                    String.class
            );
            outcome = "success";
        } finally {
            botMetrics.recordQqRequest("recall_message", outcome, System.nanoTime() - startNanos);
        }
    }

    public MediaDto uploadImage(String groupOpenId, String imageUrl) {
        return uploadMedia(groupOpenId, imageUrl, IMAGE_FILE_TYPE, "图片", "upload_image");
    }

    public MediaDto uploadImageFile(String groupOpenId, Path imagePath) {
        return uploadLocalFile(groupOpenId, imagePath, IMAGE_FILE_TYPE, "图片", "upload_image_file");
    }

    public MediaDto uploadAudio(String groupOpenId, String audioUrl) {
        return uploadMedia(groupOpenId, audioUrl, AUDIO_FILE_TYPE, "语音", "upload_audio");
    }

    public MediaDto uploadAudioFile(String groupOpenId, Path audioPath) {
        return uploadLocalFile(groupOpenId, audioPath, AUDIO_FILE_TYPE, "语音", "upload_audio_file");
    }

    public QqMediaUploadPrepareResultDto prepareUpload(String groupOpenId,
                                                       QqMediaUploadPrepareRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("QQ 群富媒体预上传请求不能为空");
        }
        Map<String, Object> requestBody = ObjectMapperUtil.getObjectMapper().convertValue(request, Map.class);
        return openApiClient.post(
                String.format(GROUP_UPLOAD_PREPARE_PATH, safeGroupOpenId(groupOpenId)),
                requestBody,
                QqMediaUploadPrepareResultDto.class
        );
    }

    public void finishUploadPart(String groupOpenId, QqMediaUploadPartFinishRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("QQ 群富媒体分片完成请求不能为空");
        }
        Map<String, Object> requestBody = ObjectMapperUtil.getObjectMapper().convertValue(request, Map.class);
        openApiClient.post(
                String.format(GROUP_UPLOAD_PART_FINISH_PATH, safeGroupOpenId(groupOpenId)),
                requestBody,
                String.class
        );
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
                    String.format(GROUP_FILE_PATH, safeGroupOpenId(groupOpenId)), request, TxFileUploadResultDto.class);
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

    private MediaDto uploadLocalFile(String groupOpenId, Path filePath, int fileType,
                                     String mediaName, String operation) {
        long startNanos = System.nanoTime();
        String outcome = "exception";
        try {
            String safeGroupOpenId = safeGroupOpenId(groupOpenId);
            Path normalizedFile = validateLocalMediaFile(filePath, mediaName);
            long fileSize = Files.size(normalizedFile);
            FileHashes hashes = calculateFileHashes(normalizedFile, fileSize);
            QqMediaUploadPrepareRequestDto prepareRequest = new QqMediaUploadPrepareRequestDto();
            prepareRequest.setFileType(fileType);
            prepareRequest.setFileName(normalizedFile.getFileName().toString());
            prepareRequest.setFileSize(String.valueOf(fileSize));
            prepareRequest.setMd5(hashes.md5());
            prepareRequest.setSha1(hashes.sha1());
            prepareRequest.setMd5TenMb(hashes.md5TenMb());

            log.info("QQ 本地富媒体分片上传开始，mediaName=>{}，fileName=>{}，bytes=>{}，md5=>{}，sha1=>{}",
                    mediaName, normalizedFile.getFileName(), fileSize, hashes.md5(), hashes.sha1());
            QqMediaUploadPrepareResultDto prepareResult = prepareUpload(safeGroupOpenId, prepareRequest);
            requireUploadId(prepareResult, mediaName);
            uploadPreparedParts(safeGroupOpenId, normalizedFile, fileSize, prepareResult);
            MediaDto media = completeChunkUpload(safeGroupOpenId, prepareResult.getUploadId(), fileType, mediaName);
            outcome = "success";
            log.info("QQ 本地富媒体分片上传完成，mediaName=>{}，hasUploadId=>{}，fileName=>{}",
                    mediaName, prepareResult.getUploadId() != null && !prepareResult.getUploadId().isBlank(), normalizedFile.getFileName());
            return media;
        } catch (IOException e) {
            throw new RuntimeException("QQ " + mediaName + "本地文件读取失败", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("QQ " + mediaName + "分片上传被中断", e);
        } finally {
            botMetrics.recordQqRequest(operation, outcome, System.nanoTime() - startNanos);
        }
    }

    private MediaDto completeChunkUpload(String groupOpenId, String uploadId, int fileType, String mediaName) {
        Map<String, Object> request = Map.of(
                "file_type", fileType,
                "upload_id", uploadId,
                "srv_send_msg", false
        );
        TxFileUploadResultDto uploadResult = openApiClient.post(
                String.format(GROUP_FILE_PATH, safeGroupOpenId(groupOpenId)), request, TxFileUploadResultDto.class);
        if (uploadResult == null || uploadResult.getFileInfo() == null || uploadResult.getFileInfo().isBlank()) {
            throw QqOpenApiErrorMapper.invalidResponse("QQ " + mediaName + "分片上传完成");
        }
        return new MediaDto(uploadResult.getFileInfo());
    }

    private Path validateLocalMediaFile(Path filePath, String mediaName) throws IOException {
        if (filePath == null) {
            throw new IllegalArgumentException("QQ " + mediaName + "本地文件不能为空");
        }
        Path normalizedFile = filePath.toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalizedFile)) {
            throw new IllegalArgumentException("QQ " + mediaName + "本地文件不存在：" + normalizedFile);
        }
        long fileSize = Files.size(normalizedFile);
        if (fileSize <= 0) {
            throw new IllegalArgumentException("QQ " + mediaName + "本地文件不能为空文件");
        }
        if (fileSize > MAX_MEDIA_BYTES) {
            throw new IllegalArgumentException("QQ " + mediaName + "本地文件不能超过 200MB：" + fileSize);
        }
        return normalizedFile;
    }

    private void requireUploadId(QqMediaUploadPrepareResultDto prepareResult, String mediaName) {
        if (prepareResult == null || prepareResult.getUploadId() == null || prepareResult.getUploadId().isBlank()) {
            throw QqOpenApiErrorMapper.invalidResponse("QQ " + mediaName + "分片预上传");
        }
    }

    private void uploadPreparedParts(String groupOpenId, Path filePath, long fileSize,
                                     QqMediaUploadPrepareResultDto prepareResult)
            throws IOException, InterruptedException {
        List<QqMediaUploadPrepareResultDto.UploadPart> parts = prepareResult.getParts();
        if (parts == null || parts.isEmpty()) {
            log.info("QQ 分片预上传未返回分片，跳过 PUT，hasUploadId=>{}",
                    prepareResult.getUploadId() != null && !prepareResult.getUploadId().isBlank());
            return;
        }
        List<QqMediaUploadPrepareResultDto.UploadPart> orderedParts = new ArrayList<>(parts);
        orderedParts.sort(Comparator.comparingInt(part -> part.getIndex() == null ? Integer.MAX_VALUE : part.getIndex()));
        long offset = 0;
        Long defaultBlockSize = parsePositiveLongOrNull(prepareResult.getBlockSize());
        for (QqMediaUploadPrepareResultDto.UploadPart part : orderedParts) {
            requirePart(part, prepareResult.getUploadId());
            long remaining = fileSize - offset;
            if (remaining <= 0) {
                break;
            }
            long partSize = resolvePartSize(part, defaultBlockSize, remaining);
            byte[] bytes = readChunk(filePath, offset, partSize);
            String partMd5 = digest("MD5", bytes);
            putPresignedPart(part.getPresignedUrl(), bytes, part.getIndex(),
                    prepareResult.getUploadId(), partMd5, PRESIGNED_UPLOAD_TIMEOUT);

            QqMediaUploadPartFinishRequestDto finishRequest = new QqMediaUploadPartFinishRequestDto();
            finishRequest.setUploadId(prepareResult.getUploadId());
            finishRequest.setPartIndex(part.getIndex());
            finishRequest.setBlockSize(String.valueOf(bytes.length));
            finishRequest.setMd5(partMd5);
            finishUploadPart(groupOpenId, finishRequest);
            offset += bytes.length;
        }
        if (offset != fileSize) {
            throw QqOpenApiErrorMapper.invalidResponse("QQ 分片预上传返回的分片数量不足");
        }
    }

    private void requirePart(QqMediaUploadPrepareResultDto.UploadPart part, String uploadId) {
        if (part == null || part.getIndex() == null) {
            throw QqOpenApiErrorMapper.invalidResponse("QQ 分片预上传返回了缺少 index 的分片，hasUploadId=" + (uploadId != null && !uploadId.isBlank()));
        }
        if (part.getPresignedUrl() == null || part.getPresignedUrl().isBlank()) {
            throw QqOpenApiErrorMapper.invalidResponse("QQ 分片预上传返回了缺少 presigned_url 的分片，hasUploadId=" + (uploadId != null && !uploadId.isBlank()));
        }
    }

    private long resolvePartSize(QqMediaUploadPrepareResultDto.UploadPart part,
                                 Long defaultBlockSize, long remaining) {
        Long partBlockSize = parsePositiveLongOrNull(part.getBlockSize());
        long size = partBlockSize != null ? partBlockSize : (defaultBlockSize == null ? remaining : defaultBlockSize);
        return Math.min(size, remaining);
    }

    private Long parsePositiveLongOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        long parsed = Long.parseLong(value.trim());
        if (parsed <= 0) {
            return null;
        }
        return parsed;
    }

    private byte[] readChunk(Path filePath, long offset, long length) throws IOException {
        if (length > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("QQ 分片大小超过 JVM 单次读取限制：" + length);
        }
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            long skipped = inputStream.skip(offset);
            while (skipped < offset) {
                long current = inputStream.skip(offset - skipped);
                if (current <= 0) {
                    throw new IOException("读取 QQ 上传分片时无法跳过到指定偏移：" + offset);
                }
                skipped += current;
            }
            byte[] bytes = inputStream.readNBytes((int) length);
            if (bytes.length != length) {
                throw new IOException("读取 QQ 上传分片长度不足，expected=" + length + "，actual=" + bytes.length);
            }
            return bytes;
        }
    }

    protected void putPresignedPart(String presignedUrl, byte[] bytes, Integer partIndex,
                                    String uploadId, String partMd5, Duration timeout)
            throws IOException, InterruptedException {
        URI uri = URI.create(presignedUrl);
        long startNanos = System.nanoTime();
        log.info("外部 HTTP 请求开始，method=>PUT，service=>QQMediaPresignedUpload，host=>{}，partIndex=>{}，hasUploadId=>{}，bytes=>{}，md5=>{}",
                uri.getHost(), partIndex, uploadId != null && !uploadId.isBlank(), bytes.length, partMd5);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .PUT(HttpRequest.BodyPublishers.ofByteArray(bytes))
                .build();
        OutboundHttpRateLimiter.awaitPermit();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        long elapsedMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.error("外部 HTTP 请求返回错误，method=>PUT，service=>QQMediaPresignedUpload，host=>{}，partIndex=>{}，hasUploadId=>{}，status=>{}，elapsedMs=>{}，responseBody=>{}",
                    uri.getHost(), partIndex, uploadId != null && !uploadId.isBlank(), response.statusCode(), elapsedMs,
                    com.grafie.botjava.util.SensitiveDataUtil.redactText(response.body()));
            throw new RuntimeException("QQ 富媒体分片 PUT 失败，status=" + response.statusCode());
        }
        log.info("外部 HTTP 请求完成，method=>PUT，service=>QQMediaPresignedUpload，host=>{}，partIndex=>{}，hasUploadId=>{}，status=>{}，elapsedMs=>{}",
                uri.getHost(), partIndex, uploadId != null && !uploadId.isBlank(), response.statusCode(), elapsedMs);
    }

    private FileHashes calculateFileHashes(Path filePath, long fileSize) throws IOException {
        MessageDigest md5 = messageDigest("MD5");
        MessageDigest sha1 = messageDigest("SHA-1");
        MessageDigest md5TenMb = messageDigest("MD5");
        long remainingTenMb = Math.min(fileSize, MD5_TEN_MB_BYTES);
        byte[] buffer = new byte[8192];
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                md5.update(buffer, 0, read);
                sha1.update(buffer, 0, read);
                if (remainingTenMb > 0) {
                    int md5TenMbBytes = (int) Math.min(read, remainingTenMb);
                    md5TenMb.update(buffer, 0, md5TenMbBytes);
                    remainingTenMb -= md5TenMbBytes;
                }
            }
        }
        return new FileHashes(toHex(md5.digest()), toHex(sha1.digest()), toHex(md5TenMb.digest()));
    }

    private MessageDigest messageDigest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JVM 不支持摘要算法：" + algorithm, e);
        }
    }

    private String digest(String algorithm, byte[] bytes) {
        MessageDigest digest = messageDigest(algorithm);
        digest.update(bytes);
        return toHex(digest.digest());
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value & 0xff));
        }
        return builder.toString();
    }

    private String safeGroupOpenId(String groupOpenId) {
        if (groupOpenId == null || groupOpenId.isBlank()) {
            throw new IllegalArgumentException("QQ 群 OpenID 不能为空");
        }
        String trimmed = groupOpenId.trim();
        if (trimmed.contains("/")) {
            throw new IllegalArgumentException("QQ 群 OpenID 不能包含路径分隔符");
        }
        return trimmed;
    }

    private Map<String, Object> pagingQuery(String cursor, Integer limit) {
        Map<String, Object> query = new LinkedHashMap<>();
        if (cursor != null && !cursor.isBlank()) {
            query.put("cursor", cursor.trim());
        }
        if (limit != null) {
            query.put("limit", limit);
        }
        return query;
    }

    private Map<String, Object> requestBody(Object request, String message) {
        if (request == null) {
            throw new IllegalArgumentException(message);
        }
        return ObjectMapperUtil.getObjectMapper().convertValue(request, Map.class);
    }

    private String safeMemberOpenId(String memberOpenId) {
        return safePathSegment(memberOpenId, "QQ member OpenID must not be blank", "QQ member OpenID must not contain path separator");
    }

    private String safeStrategyId(String strategyId) {
        return safePathSegment(strategyId, "QQ join approval strategy id must not be blank", "QQ join approval strategy id must not contain path separator");
    }

    private String safePathSegment(String value, String blankMessage, String slashMessage) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(blankMessage);
        }
        String trimmed = value.trim();
        if (trimmed.contains("/")) {
            throw new IllegalArgumentException(slashMessage);
        }
        return trimmed;
    }

    private String safeMessageId(String messageId) {
        if (messageId == null || messageId.isBlank()) {
            throw new IllegalArgumentException("QQ 群消息 ID 不能为空");
        }
        String trimmed = messageId.trim();
        if (trimmed.contains("/")) {
            throw new IllegalArgumentException("QQ 群消息 ID 不能包含路径分隔符");
        }
        return trimmed;
    }

    private record FileHashes(String md5, String sha1, String md5TenMb) {
    }
}
