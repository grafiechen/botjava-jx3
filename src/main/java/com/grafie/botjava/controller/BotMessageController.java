package com.grafie.botjava.controller;

import com.grafie.botjava.config.QqIngressProperties;
import com.grafie.botjava.contants.OpCode;
import com.grafie.botjava.entity.dto.payload.Payload;
import com.grafie.botjava.qq.QqCallbackSignatureVerifier;
import com.grafie.botjava.service.BotMessageService;
import com.grafie.botjava.util.ObjectMapperUtil;
import com.grafie.botjava.util.SensitiveDataUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author grafie.chen
 * @since 2025/1/22  10:50
 */
@Slf4j
@RequestMapping("/bot/message")
@RestController
public class BotMessageController {
    private static final String HEADER_SIGNATURE = "X-Signature-Ed25519";
    private static final String HEADER_TIMESTAMP = "X-Signature-Timestamp";

    private final BotMessageService botMessageService;
    private final QqCallbackSignatureVerifier signatureVerifier;
    private final QqIngressProperties ingressProperties;

    public BotMessageController(BotMessageService botMessageService,
                                QqCallbackSignatureVerifier signatureVerifier,
                                QqIngressProperties ingressProperties) {
        this.botMessageService = botMessageService;
        this.signatureVerifier = signatureVerifier;
        this.ingressProperties = ingressProperties;
    }

    @PostMapping()
    public ResponseEntity<Object> getMessage(
            @RequestBody byte[] rawBody,
            @RequestHeader(value = HEADER_SIGNATURE, required = false) String signature,
            @RequestHeader(value = HEADER_TIMESTAMP, required = false) String timestamp) {
        if (!ingressProperties.isWebhookEnabled()) {
            log.warn("QQ webhook 入站未启用，messageIngressMode=>{}", ingressProperties.getMessageIngressMode());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        if (rawBody == null || rawBody.length == 0) {
            return ResponseEntity.badRequest().build();
        }
        if (rawBody.length > ingressProperties.getWebhookMaxBodyBytes()) {
            log.warn("QQ webhook 请求体超过限制，bodyBytes=>{}，maximum=>{}",
                    rawBody.length, ingressProperties.getWebhookMaxBodyBytes());
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
        }
        Payload payload = null;
        try {
            payload = ObjectMapperUtil.getObjectMapper().readValue(rawBody, Payload.class);
            log.info("接收到 QQ 推送，op=>{}，t=>{}，sequence=>{}",
                    payload.getOp(), payload.getT(), payload.getS());
            if (requiresSignature(payload)
                    && !signatureVerifier.verify(timestamp, signature, rawBody)) {
                log.warn("QQ 推送签名校验失败，op=>{}，t=>{}，sequence=>{}",
                        payload.getOp(), payload.getT(), payload.getS());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            return ResponseEntity.ok(botMessageService.dealMessage(payload));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("解析 QQ 推送失败，reason=>{}", SensitiveDataUtil.summarize(e), e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("处理 QQ 推送失败，op=>{}，t=>{}，reason=>{}",
                    payload == null ? null : payload.getOp(),
                    payload == null ? null : payload.getT(),
                    SensitiveDataUtil.summarize(e), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private boolean requiresSignature(Payload payload) {
        return payload == null || !OpCode.CALLBACK_VALID.getCode().equals(payload.getOp());
    }
}
