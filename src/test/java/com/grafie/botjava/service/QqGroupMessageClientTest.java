package com.grafie.botjava.service;

import com.grafie.botjava.entity.dto.TxFileUploadResultDto;
import com.grafie.botjava.entity.dto.common.MediaDto;
import com.grafie.botjava.entity.dto.common.TxMessageInfo;
import com.grafie.botjava.entity.dto.qq.QqBotGroupStateDto;
import com.grafie.botjava.entity.dto.qq.QqGroupMessageSendResultDto;
import com.grafie.botjava.entity.dto.qq.QqGroupInfoDto;
import com.grafie.botjava.entity.dto.qq.QqMediaUploadPartFinishRequestDto;
import com.grafie.botjava.entity.dto.qq.QqMediaUploadPrepareRequestDto;
import com.grafie.botjava.entity.dto.qq.QqMediaUploadPrepareResultDto;
import com.grafie.botjava.qq.QqOpenApiClient;
import com.grafie.botjava.qq.QqOpenApiException;
import com.grafie.botjava.observability.BotMetrics;
import com.grafie.botjava.observability.RequestTraceContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QqGroupMessageClientTest {

    @Test
    void shouldSendMessageToGroupOpenApiPath() {
        QqOpenApiClient openApiClient = mock(QqOpenApiClient.class);
        BotMetrics metrics = mock(BotMetrics.class);
        AtomicReference<String> traceDuringRequest = new AtomicReference<>();
        when(openApiClient.post(
                eq("/v2/groups/group-1/messages"),
                org.mockito.ArgumentMatchers.anyMap(),
                eq(QqGroupMessageSendResultDto.class)
        )).thenAnswer(invocation -> {
            traceDuringRequest.set(RequestTraceContext.currentId().orElse(null));
            QqGroupMessageSendResultDto result = new QqGroupMessageSendResultDto();
            result.setId("sent-message-1");
            return result;
        });
        QqGroupMessageClient client = new QqGroupMessageClient(openApiClient, metrics);
        TxMessageInfo message = new TxMessageInfo();
        message.setMsg_type(0);
        message.setContent("开服啦");
        message.setMsg_id("message-1");

        try (RequestTraceContext.Scope ignored = RequestTraceContext.open("invocation-qq-1")) {
            client.send("group-1", message);
        }

        ArgumentCaptor<Map<String, Object>> bodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(openApiClient).post(eq("/v2/groups/group-1/messages"), bodyCaptor.capture(),
                eq(QqGroupMessageSendResultDto.class));
        assertEquals(0, bodyCaptor.getValue().get("msg_type"));
        assertEquals("开服啦", bodyCaptor.getValue().get("content"));
        assertEquals("message-1", bodyCaptor.getValue().get("msg_id"));
        verify(metrics).recordQqRequest(eq("send_message"), eq("success"),
                org.mockito.ArgumentMatchers.anyLong());
        assertEquals("invocation-qq-1", traceDuringRequest.get());
        assertFalse(RequestTraceContext.currentId().isPresent());
    }

    @Test
    void shouldRecallGroupMessageFromOfficialPath() {
        QqOpenApiClient openApiClient = mock(QqOpenApiClient.class);
        BotMetrics metrics = mock(BotMetrics.class);
        QqGroupMessageClient client = new QqGroupMessageClient(openApiClient, metrics);

        client.recallMessage(" group-1 ", " message-1 ");

        verify(openApiClient).delete(eq("/v2/groups/group-1/messages/message-1"), eq(Map.of()), eq(String.class));
        verify(metrics).recordQqRequest(eq("recall_message"), eq("success"),
                org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void shouldFetchGroupInfoAndBotStateFromOfficialV2Paths() {
        QqOpenApiClient openApiClient = mock(QqOpenApiClient.class);
        BotMetrics metrics = mock(BotMetrics.class);
        QqGroupInfoDto groupInfo = new QqGroupInfoDto();
        groupInfo.setGroupName("测试群");
        QqBotGroupStateDto botState = new QqBotGroupStateDto();
        botState.setAllowProactiveMsg(true);
        when(openApiClient.get(eq("/v2/groups/group-1/info"), eq(Map.of()), eq(QqGroupInfoDto.class)))
                .thenReturn(groupInfo);
        when(openApiClient.get(eq("/v2/groups/group-1/bot_state"), eq(Map.of()), eq(QqBotGroupStateDto.class)))
                .thenReturn(botState);
        QqGroupMessageClient client = new QqGroupMessageClient(openApiClient, metrics);

        assertEquals("测试群", client.getGroupInfo(" group-1 ").getGroupName());
        assertEquals(true, client.getBotState("group-1").getAllowProactiveMsg());
        verify(openApiClient).get(eq("/v2/groups/group-1/info"), eq(Map.of()), eq(QqGroupInfoDto.class));
        verify(openApiClient).get(eq("/v2/groups/group-1/bot_state"), eq(Map.of()), eq(QqBotGroupStateDto.class));
    }

    @Test
    void shouldExposeGroupChunkUploadEndpoints() {
        QqOpenApiClient openApiClient = mock(QqOpenApiClient.class);
        BotMetrics metrics = mock(BotMetrics.class);
        QqMediaUploadPrepareResultDto prepareResult = new QqMediaUploadPrepareResultDto();
        prepareResult.setUploadId("upload-1");
        when(openApiClient.post(eq("/v2/groups/group-1/upload_prepare"), anyMap(),
                eq(QqMediaUploadPrepareResultDto.class))).thenReturn(prepareResult);
        QqGroupMessageClient client = new QqGroupMessageClient(openApiClient, metrics);
        QqMediaUploadPrepareRequestDto prepareRequest = new QqMediaUploadPrepareRequestDto();
        prepareRequest.setFileType(2);
        prepareRequest.setFileName("demo.mp4");
        prepareRequest.setFileSize("31457280");
        QqMediaUploadPartFinishRequestDto finishRequest = new QqMediaUploadPartFinishRequestDto();
        finishRequest.setUploadId("upload-1");
        finishRequest.setPartIndex(0);

        assertEquals("upload-1", client.prepareUpload("group-1", prepareRequest).getUploadId());
        client.finishUploadPart("group-1", finishRequest);

        ArgumentCaptor<Map<String, Object>> prepareBodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(openApiClient).post(eq("/v2/groups/group-1/upload_prepare"), prepareBodyCaptor.capture(),
                eq(QqMediaUploadPrepareResultDto.class));
        assertEquals(2, prepareBodyCaptor.getValue().get("file_type"));
        assertEquals("demo.mp4", prepareBodyCaptor.getValue().get("file_name"));
        verify(openApiClient).post(eq("/v2/groups/group-1/upload_part_finish"), anyMap(), eq(String.class));
    }

    @Test
    void shouldUploadLocalImageByChunkAndReturnMediaFileInfo(@TempDir Path tempDir) throws Exception {
        Path image = tempDir.resolve("demo.png");
        Files.writeString(image, "abcdefghi", StandardCharsets.UTF_8);
        QqOpenApiClient openApiClient = mock(QqOpenApiClient.class);
        BotMetrics metrics = mock(BotMetrics.class);
        QqMediaUploadPrepareResultDto prepareResult = new QqMediaUploadPrepareResultDto();
        prepareResult.setUploadId("upload-image-1");
        prepareResult.setBlockSize("4");
        prepareResult.setParts(List.of(uploadPart(0, "https://upload.example.com/0"),
                uploadPart(1, "https://upload.example.com/1"),
                uploadPart(2, "https://upload.example.com/2")));
        when(openApiClient.post(eq("/v2/groups/group-1/upload_prepare"), anyMap(),
                eq(QqMediaUploadPrepareResultDto.class))).thenReturn(prepareResult);
        when(openApiClient.post(eq("/v2/groups/group-1/upload_part_finish"), anyMap(), eq(String.class)))
                .thenReturn("ok");
        TxFileUploadResultDto uploadResult = new TxFileUploadResultDto();
        uploadResult.setFileInfo("LOCAL_IMAGE_FILE_INFO");
        when(openApiClient.post(eq("/v2/groups/group-1/files"), anyMap(), eq(TxFileUploadResultDto.class)))
                .thenReturn(uploadResult);
        CapturingQqGroupMessageClient client = new CapturingQqGroupMessageClient(openApiClient, metrics);

        MediaDto media = client.uploadImageFile("group-1", image);

        assertEquals("LOCAL_IMAGE_FILE_INFO", media.getFile_info());
        assertEquals(List.of("abcd", "efgh", "i"), client.uploadedChunks);
        ArgumentCaptor<Map<String, Object>> prepareBodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(openApiClient).post(eq("/v2/groups/group-1/upload_prepare"), prepareBodyCaptor.capture(),
                eq(QqMediaUploadPrepareResultDto.class));
        assertEquals(1, prepareBodyCaptor.getValue().get("file_type"));
        assertEquals("demo.png", prepareBodyCaptor.getValue().get("file_name"));
        assertEquals("9", prepareBodyCaptor.getValue().get("file_size"));
        assertEquals(hex("MD5", "abcdefghi"), prepareBodyCaptor.getValue().get("md5"));
        assertEquals(hex("SHA-1", "abcdefghi"), prepareBodyCaptor.getValue().get("sha1"));
        assertEquals(hex("MD5", "abcdefghi"), prepareBodyCaptor.getValue().get("md5_10m"));

        ArgumentCaptor<Map<String, Object>> finishBodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(openApiClient, times(3)).post(eq("/v2/groups/group-1/upload_part_finish"),
                finishBodyCaptor.capture(), eq(String.class));
        assertEquals(0, finishBodyCaptor.getAllValues().get(0).get("part_index"));
        assertEquals("4", finishBodyCaptor.getAllValues().get(0).get("block_size"));
        assertEquals(1, finishBodyCaptor.getAllValues().get(1).get("part_index"));
        assertEquals("4", finishBodyCaptor.getAllValues().get(1).get("block_size"));
        assertEquals(2, finishBodyCaptor.getAllValues().get(2).get("part_index"));
        assertEquals("1", finishBodyCaptor.getAllValues().get(2).get("block_size"));

        ArgumentCaptor<Map<String, Object>> completeBodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(openApiClient).post(eq("/v2/groups/group-1/files"), completeBodyCaptor.capture(),
                eq(TxFileUploadResultDto.class));
        assertEquals(1, completeBodyCaptor.getValue().get("file_type"));
        assertEquals("upload-image-1", completeBodyCaptor.getValue().get("upload_id"));
        assertEquals(false, completeBodyCaptor.getValue().get("srv_send_msg"));
        verify(metrics).recordQqRequest(eq("upload_image_file"), eq("success"),
                org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void shouldRejectLocalImageWhenFileMissing(@TempDir Path tempDir) {
        QqGroupMessageClient client = new QqGroupMessageClient(mock(QqOpenApiClient.class), mock(BotMetrics.class));

        assertThrows(IllegalArgumentException.class,
                () -> client.uploadImageFile("group-1", tempDir.resolve("missing.png")));
    }

    @Test
    void shouldUploadImageAndReturnMediaFileInfo() {
        QqOpenApiClient openApiClient = mock(QqOpenApiClient.class);
        BotMetrics metrics = mock(BotMetrics.class);
        TxFileUploadResultDto uploadResult = new TxFileUploadResultDto();
        uploadResult.setFileInfo("FILE_INFO_001");
        when(openApiClient.post(eq("/v2/groups/group-1/files"), anyMap(), eq(TxFileUploadResultDto.class)))
                .thenReturn(uploadResult);
        QqGroupMessageClient client = new QqGroupMessageClient(openApiClient, metrics);

        MediaDto media = client.uploadImage("group-1", "https://img.example.com/result.png");

        assertEquals("FILE_INFO_001", media.getFile_info());
        ArgumentCaptor<Map<String, Object>> bodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(openApiClient).post(eq("/v2/groups/group-1/files"), bodyCaptor.capture(),
                eq(TxFileUploadResultDto.class));
        assertEquals(1, bodyCaptor.getValue().get("file_type"));
        assertEquals("https://img.example.com/result.png", bodyCaptor.getValue().get("url"));
        assertEquals(false, bodyCaptor.getValue().get("srv_send_msg"));
        verify(metrics).recordQqRequest(eq("upload_image"), eq("success"),
                org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void shouldUploadAudioWithFileTypeThree() {
        QqOpenApiClient openApiClient = mock(QqOpenApiClient.class);
        TxFileUploadResultDto uploadResult = new TxFileUploadResultDto();
        uploadResult.setFileInfo("AUDIO_FILE_INFO");
        when(openApiClient.post(eq("/v2/groups/group-1/files"), anyMap(), eq(TxFileUploadResultDto.class)))
                .thenReturn(uploadResult);
        QqGroupMessageClient client = new QqGroupMessageClient(openApiClient, mock(BotMetrics.class));

        MediaDto media = client.uploadAudio("group-1", "https://audio.example.com/result.mp3");

        assertEquals("AUDIO_FILE_INFO", media.getFile_info());
        ArgumentCaptor<Map<String, Object>> bodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(openApiClient).post(eq("/v2/groups/group-1/files"), bodyCaptor.capture(),
                eq(TxFileUploadResultDto.class));
        assertEquals(3, bodyCaptor.getValue().get("file_type"));
    }

    @Test
    void shouldRejectUploadWithoutFileInfo() {
        QqOpenApiClient openApiClient = mock(QqOpenApiClient.class);
        BotMetrics metrics = mock(BotMetrics.class);
        when(openApiClient.post(eq("/v2/groups/group-1/files"), anyMap(), eq(TxFileUploadResultDto.class)))
                .thenReturn(new TxFileUploadResultDto());
        QqGroupMessageClient client = new QqGroupMessageClient(openApiClient, metrics);

        assertThrows(
                QqOpenApiException.class,
                () -> client.uploadImage("group-1", "https://img.example.com/result.png")
        );
        verify(metrics).recordQqRequest(eq("upload_image"), eq("invalid_response"),
                org.mockito.ArgumentMatchers.anyLong());
    }

    private static QqMediaUploadPrepareResultDto.UploadPart uploadPart(int index, String presignedUrl) {
        QqMediaUploadPrepareResultDto.UploadPart part = new QqMediaUploadPrepareResultDto.UploadPart();
        part.setIndex(index);
        part.setPresignedUrl(presignedUrl);
        part.setBlockSize("4");
        return part;
    }

    private static String hex(String algorithm, String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static class CapturingQqGroupMessageClient extends QqGroupMessageClient {

        private final List<String> uploadedChunks = new ArrayList<>();

        private CapturingQqGroupMessageClient(QqOpenApiClient openApiClient, BotMetrics botMetrics) {
            super(openApiClient, botMetrics);
        }

        @Override
        protected void putPresignedPart(String presignedUrl, byte[] bytes, Integer partIndex,
                                        String uploadId, String partMd5, Duration timeout) {
            uploadedChunks.add(new String(bytes, StandardCharsets.UTF_8));
        }
    }
}
