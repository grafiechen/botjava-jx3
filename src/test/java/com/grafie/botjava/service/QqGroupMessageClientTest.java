package com.grafie.botjava.service;

import com.grafie.botjava.entity.dto.TxFileUploadResultDto;
import com.grafie.botjava.entity.dto.common.MediaDto;
import com.grafie.botjava.entity.dto.common.TxMessageInfo;
import com.grafie.botjava.qq.QqOpenApiClient;
import com.grafie.botjava.qq.QqOpenApiException;
import com.grafie.botjava.observability.BotMetrics;
import com.grafie.botjava.observability.RequestTraceContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
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
                eq(String.class)
        )).thenAnswer(invocation -> {
            traceDuringRequest.set(RequestTraceContext.currentId().orElse(null));
            return "ok";
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
        verify(openApiClient).post(eq("/v2/groups/group-1/messages"), bodyCaptor.capture(), eq(String.class));
        assertEquals(0, bodyCaptor.getValue().get("msg_type"));
        assertEquals("开服啦", bodyCaptor.getValue().get("content"));
        assertEquals("message-1", bodyCaptor.getValue().get("msg_id"));
        verify(metrics).recordQqRequest(eq("send_message"), eq("success"),
                org.mockito.ArgumentMatchers.anyLong());
        assertEquals("invocation-qq-1", traceDuringRequest.get());
        assertFalse(RequestTraceContext.currentId().isPresent());
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
}
