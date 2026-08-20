package com.grafie.botjava.qq;

import com.grafie.botjava.config.QqIngressProperties;
import com.grafie.botjava.config.TxBotProperty;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class QqCallbackSignatureVerifierTest {
    @Test
    void shouldAcceptFreshSecondsAndMillisecondsAndRejectStaleOrMalformedTimestamp() {
        QqIngressProperties ingress = new QqIngressProperties();
        ingress.setWebhookMaxSignatureAgeSeconds(300);
        QqCallbackSignatureVerifier verifier = new QqCallbackSignatureVerifier(new TxBotProperty(), ingress);
        Instant now = Instant.parse("2026-08-13T00:00:00Z");

        assertThat(verifier.isFresh(String.valueOf(now.minusSeconds(299).getEpochSecond()), now)).isTrue();
        assertThat(verifier.isFresh(String.valueOf(now.plusSeconds(299).toEpochMilli()), now)).isTrue();
        assertThat(verifier.isFresh(String.valueOf(now.minusSeconds(301).getEpochSecond()), now)).isFalse();
        assertThat(verifier.isFresh(String.valueOf(now.plusSeconds(301).toEpochMilli()), now)).isFalse();
        assertThat(verifier.isFresh("not-a-timestamp", now)).isFalse();
        assertThat(verifier.isFresh(null, now)).isFalse();
    }
}
