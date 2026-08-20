package com.grafie.botjava.qq;

import com.grafie.botjava.config.QqIngressProperties;
import com.grafie.botjava.config.TxBotProperty;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;

@Component
public class QqCallbackSignatureVerifier {
    private final TxBotProperty txBotProperty;
    private final QqIngressProperties ingressProperties;

    public QqCallbackSignatureVerifier(TxBotProperty txBotProperty, QqIngressProperties ingressProperties) {
        this.txBotProperty = txBotProperty;
        this.ingressProperties = ingressProperties;
    }

    public boolean verify(String timestamp, String signatureHex, byte[] rawBody) {
        return isFresh(timestamp, Instant.now())
                && QqCallbackSignatureUtil.verifyEvent(txBotProperty.getAppSecret(),
                timestamp, signatureHex, rawBody);
    }

    boolean isFresh(String timestamp, Instant now) {
        if (timestamp == null || timestamp.isBlank() || now == null) {
            return false;
        }
        try {
            long raw = Long.parseLong(timestamp.trim());
            Instant signedAt = Math.abs(raw) >= 100_000_000_000L
                    ? Instant.ofEpochMilli(raw) : Instant.ofEpochSecond(raw);
            long age = Math.abs(Duration.between(signedAt, now).getSeconds());
            return age <= ingressProperties.getWebhookMaxSignatureAgeSeconds();
        } catch (NumberFormatException | DateTimeException | ArithmeticException exception) {
            return false;
        }
    }
}
