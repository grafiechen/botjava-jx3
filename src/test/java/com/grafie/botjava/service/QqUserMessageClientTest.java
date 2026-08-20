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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QqUserMessageClientTest {

    @Test
    void shouldSendAndRecallC2cMessageOnOfficialPath() {
        QqOpenApiClient openApiClient = mock(QqOpenApiClient.class);
        BotMetrics metrics = mock(BotMetrics.class);
        QqGroupMessageSendResultDto result = new QqGroupMessageSendResultDto();
        result.setId("message-1");
        when(openApiClient.post(eq("/v2/users/user-1/messages"), anyMap(),
                eq(QqGroupMessageSendResultDto.class))).thenReturn(result);
        QqUserMessageClient client = new QqUserMessageClient(openApiClient, metrics);
        TxMessageInfo message = new TxMessageInfo();
        message.setMsg_type(0);
        message.setContent("hello");

        assertEquals("message-1", client.send(" user-1 ", message).getId());
        client.recallMessage("user-1", " message-1 ");

        ArgumentCaptor<Map<String, Object>> bodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(openApiClient).post(eq("/v2/users/user-1/messages"), bodyCaptor.capture(),
                eq(QqGroupMessageSendResultDto.class));
        assertEquals(0, bodyCaptor.getValue().get("msg_type"));
        assertEquals("hello", bodyCaptor.getValue().get("content"));
        verify(openApiClient).delete(eq("/v2/users/user-1/messages/message-1"), eq(Map.of()), eq(String.class));
        verify(metrics).recordQqRequest(eq("send_user_message"), eq("success"), org.mockito.ArgumentMatchers.anyLong());
        verify(metrics).recordQqRequest(eq("recall_user_message"), eq("success"), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void shouldStreamC2cMessage() {
        QqOpenApiClient openApiClient = mock(QqOpenApiClient.class);
        QqStreamMessageResultDto result = new QqStreamMessageResultDto();
        result.setId("stream-1");
        when(openApiClient.post(eq("/v2/users/user-1/stream_messages"), anyMap(),
                eq(QqStreamMessageResultDto.class))).thenReturn(result);
        QqUserMessageClient client = new QqUserMessageClient(openApiClient, mock(BotMetrics.class));
        QqStreamMessageRequestDto request = new QqStreamMessageRequestDto();
        request.setInputMode("replace");
        request.setInputState(1);
        request.setContentType("markdown");
        request.setContentRaw("pending");

        assertEquals("stream-1", client.stream("user-1", request).getId());

        ArgumentCaptor<Map<String, Object>> bodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(openApiClient).post(eq("/v2/users/user-1/stream_messages"), bodyCaptor.capture(),
                eq(QqStreamMessageResultDto.class));
        assertEquals("replace", bodyCaptor.getValue().get("input_mode"));
        assertEquals(1, bodyCaptor.getValue().get("input_state"));
        assertEquals("markdown", bodyCaptor.getValue().get("content_type"));
        assertEquals("pending", bodyCaptor.getValue().get("content_raw"));
    }

    @Test
    void shouldUploadC2cMediaAndExposeChunkEndpoints() {
        QqOpenApiClient openApiClient = mock(QqOpenApiClient.class);
        BotMetrics metrics = mock(BotMetrics.class);
        TxFileUploadResultDto uploadResult = new TxFileUploadResultDto();
        uploadResult.setFileInfo("FILE_INFO");
        when(openApiClient.post(eq("/v2/users/user-1/files"), anyMap(), eq(TxFileUploadResultDto.class)))
                .thenReturn(uploadResult);
        QqMediaUploadPrepareResultDto prepareResult = new QqMediaUploadPrepareResultDto();
        prepareResult.setUploadId("upload-1");
        when(openApiClient.post(eq("/v2/users/user-1/upload_prepare"), anyMap(),
                eq(QqMediaUploadPrepareResultDto.class))).thenReturn(prepareResult);
        QqUserMessageClient client = new QqUserMessageClient(openApiClient, metrics);
        QqMediaUploadPrepareRequestDto prepareRequest = new QqMediaUploadPrepareRequestDto();
        prepareRequest.setFileType(2);
        prepareRequest.setFileName("demo.mp4");
        QqMediaUploadPartFinishRequestDto finishRequest = new QqMediaUploadPartFinishRequestDto();
        finishRequest.setUploadId("upload-1");
        finishRequest.setPartIndex(0);

        MediaDto media = client.uploadImage("user-1", "https://img.example.com/a.png");
        assertEquals("FILE_INFO", media.getFile_info());
        assertEquals("upload-1", client.prepareUpload("user-1", prepareRequest).getUploadId());
        client.finishUploadPart("user-1", finishRequest);

        ArgumentCaptor<Map<String, Object>> uploadBodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(openApiClient).post(eq("/v2/users/user-1/files"), uploadBodyCaptor.capture(),
                eq(TxFileUploadResultDto.class));
        assertEquals(1, uploadBodyCaptor.getValue().get("file_type"));
        assertEquals("https://img.example.com/a.png", uploadBodyCaptor.getValue().get("url"));
        verify(openApiClient).post(eq("/v2/users/user-1/upload_part_finish"), anyMap(), eq(String.class));
        verify(metrics).recordQqRequest(eq("upload_user_image"), eq("success"), org.mockito.ArgumentMatchers.anyLong());
    }
}