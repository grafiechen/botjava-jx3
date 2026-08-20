package com.grafie.botjava.qq;

import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QqCallbackSignatureUtilTest {

    @Test
    void shouldVerifySignedEventBody() {
        String secret = "test-secret";
        String timestamp = "1784806800";
        byte[] body = "{\"op\":0,\"t\":\"GROUP_MESSAGE_CREATE\",\"d\":{}}"
                .getBytes(StandardCharsets.UTF_8);
        String signature = signEvent(secret, timestamp, body);

        assertTrue(QqCallbackSignatureUtil.verifyEvent(secret, timestamp, signature, body));
    }

    @Test
    void shouldRejectChangedBodyAndBadSignature() {
        String secret = "test-secret";
        String timestamp = "1784806800";
        byte[] body = "{\"op\":0,\"d\":{\"content\":\"test\"}}".getBytes(StandardCharsets.UTF_8);
        String signature = signEvent(secret, timestamp, body);

        assertFalse(QqCallbackSignatureUtil.verifyEvent(secret, timestamp, signature,
                "{\"op\":0,\"d\":{\"content\":\"changed\"}}".getBytes(StandardCharsets.UTF_8)));
        assertFalse(QqCallbackSignatureUtil.verifyEvent(secret, timestamp, "not-hex", body));
        assertFalse(QqCallbackSignatureUtil.verifyEvent(secret, null, signature, body));
    }

    private static String signEvent(String secret, String timestamp, byte[] body) {
        byte[] message = signedMessage(timestamp, body);
        Ed25519Signer signer = new Ed25519Signer();
        signer.init(true, privateKey(secret));
        signer.update(message, 0, message.length);
        return HexFormat.of().formatHex(signer.generateSignature());
    }

    private static byte[] signedMessage(String timestamp, byte[] body) {
        byte[] timestampBytes = timestamp.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream output = new ByteArrayOutputStream(timestampBytes.length + body.length);
        output.writeBytes(timestampBytes);
        output.writeBytes(body);
        return output.toByteArray();
    }

    private static Ed25519PrivateKeyParameters privateKey(String secret) {
        String seed = secret;
        while (seed.length() < 32) {
            seed = seed + seed;
        }
        return new Ed25519PrivateKeyParameters(
                seed.substring(0, 32).getBytes(StandardCharsets.UTF_8), 0);
    }
}
