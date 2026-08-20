package com.grafie.botjava.qq;

import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * QQ webhook Ed25519 signature helper.
 */
public final class QqCallbackSignatureUtil {
    private static final int ED25519_SEED_SIZE = 32;
    private static final int ED25519_SIGNATURE_SIZE = 64;

    private QqCallbackSignatureUtil() {
    }

    public static String signValidation(String botSecret, String eventTs, String plainToken) {
        Ed25519PrivateKeyParameters privateKey = privateKey(botSecret);
        byte[] message = (eventTs + plainToken).getBytes(StandardCharsets.UTF_8);
        Ed25519Signer signer = new Ed25519Signer();
        signer.init(true, privateKey);
        signer.update(message, 0, message.length);
        return HexFormat.of().formatHex(signer.generateSignature());
    }

    public static boolean verifyEvent(String botSecret, String timestamp,
                                      String signatureHex, byte[] rawBody) {
        if (isBlank(botSecret) || isBlank(timestamp) || isBlank(signatureHex)
                || rawBody == null || rawBody.length == 0) {
            return false;
        }
        byte[] signature;
        try {
            signature = HexFormat.of().parseHex(signatureHex.trim());
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (signature.length != ED25519_SIGNATURE_SIZE || (signature[63] & 224) != 0) {
            return false;
        }

        Ed25519PublicKeyParameters publicKey = privateKey(botSecret).generatePublicKey();
        byte[] message = signedMessage(timestamp, rawBody);
        Ed25519Signer verifier = new Ed25519Signer();
        verifier.init(false, publicKey);
        verifier.update(message, 0, message.length);
        return verifier.verifySignature(signature);
    }

    private static byte[] signedMessage(String timestamp, byte[] rawBody) {
        byte[] timestampBytes = timestamp.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream output = new ByteArrayOutputStream(timestampBytes.length + rawBody.length);
        output.writeBytes(timestampBytes);
        output.writeBytes(rawBody);
        return output.toByteArray();
    }

    private static Ed25519PrivateKeyParameters privateKey(String botSecret) {
        String seed = botSecret;
        while (seed.length() < ED25519_SEED_SIZE) {
            seed = seed + seed;
        }
        byte[] seedBytes = seed.substring(0, ED25519_SEED_SIZE).getBytes(StandardCharsets.UTF_8);
        return new Ed25519PrivateKeyParameters(seedBytes, 0);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
