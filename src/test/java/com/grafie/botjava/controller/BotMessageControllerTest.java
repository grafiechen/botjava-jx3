package com.grafie.botjava.controller;

import com.grafie.botjava.config.QqIngressProperties;
import com.grafie.botjava.contants.OpCode;
import com.grafie.botjava.entity.dto.payload.Payload;
import com.grafie.botjava.qq.QqCallbackSignatureVerifier;
import com.grafie.botjava.service.BotMessageService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BotMessageControllerTest {

    @Test
    void shouldDispatchSignedEventWhenWebhookIngressEnabled() {
        BotMessageService service = mock(BotMessageService.class);
        QqCallbackSignatureVerifier verifier = mock(QqCallbackSignatureVerifier.class);
        BotMessageController controller = new BotMessageController(service, verifier, hookIngress());
        byte[] body = """
                {"op":0,"s":1,"t":"GROUP_MESSAGE_CREATE","d":{"content":"test"}}
                """.getBytes(StandardCharsets.UTF_8);
        when(verifier.verify("ts", "sig", body)).thenReturn(true);
        when(service.dealMessage(any(Payload.class))).thenReturn("handled");

        ResponseEntity<Object> response = controller.getMessage(body, "sig", "ts");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("handled", response.getBody());
        ArgumentCaptor<Payload> payloadCaptor = ArgumentCaptor.forClass(Payload.class);
        verify(service).dealMessage(payloadCaptor.capture());
        assertEquals(OpCode.DISPATCH.getCode(), payloadCaptor.getValue().getOp());
    }

    @Test
    void shouldRejectUnsignedEventBeforeDispatchWhenWebhookIngressEnabled() {
        BotMessageService service = mock(BotMessageService.class);
        QqCallbackSignatureVerifier verifier = mock(QqCallbackSignatureVerifier.class);
        BotMessageController controller = new BotMessageController(service, verifier, hookIngress());
        byte[] body = """
                {"op":0,"s":1,"t":"GROUP_MESSAGE_CREATE","d":{"content":"test"}}
                """.getBytes(StandardCharsets.UTF_8);
        when(verifier.verify(null, null, body)).thenReturn(false);

        ResponseEntity<Object> response = controller.getMessage(body, null, null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(service, never()).dealMessage(any(Payload.class));
    }

    @Test
    void shouldBypassEventSignatureForCallbackValidationChallengeWhenWebhookIngressEnabled() {
        BotMessageService service = mock(BotMessageService.class);
        QqCallbackSignatureVerifier verifier = mock(QqCallbackSignatureVerifier.class);
        BotMessageController controller = new BotMessageController(service, verifier, hookIngress());
        byte[] body = """
                {"op":13,"d":{"plain_token":"plain","event_ts":"1784806800"}}
                """.getBytes(StandardCharsets.UTF_8);
        when(service.dealMessage(any(Payload.class))).thenReturn("signature-response");

        ResponseEntity<Object> response = controller.getMessage(body, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("signature-response", response.getBody());
        verify(service).dealMessage(any(Payload.class));
        verifyNoInteractions(verifier);
    }

    @Test
    void shouldDisableWebhookWhenIngressModeIsWebSocket() {
        BotMessageService service = mock(BotMessageService.class);
        QqCallbackSignatureVerifier verifier = mock(QqCallbackSignatureVerifier.class);
        BotMessageController controller = new BotMessageController(service, verifier, new QqIngressProperties());

        ResponseEntity<Object> response = controller.getMessage(
                "{}".getBytes(StandardCharsets.UTF_8), null, null);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verifyNoInteractions(service, verifier);
    }

    @Test
    void shouldReturnBadRequestForInvalidJsonWhenWebhookIngressEnabled() {
        BotMessageService service = mock(BotMessageService.class);
        QqCallbackSignatureVerifier verifier = mock(QqCallbackSignatureVerifier.class);
        BotMessageController controller = new BotMessageController(service, verifier, hookIngress());

        ResponseEntity<Object> response = controller.getMessage(
                "{bad-json}".getBytes(StandardCharsets.UTF_8), null, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(service, verifier);
    }

    private static QqIngressProperties hookIngress() {
        QqIngressProperties properties = new QqIngressProperties();
        properties.setMessageIngressMode(QqIngressProperties.MessageIngressMode.HOOK);
        return properties;
    }
}
