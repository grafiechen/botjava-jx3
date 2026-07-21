package com.grafie.botjava.qq;

import java.net.URI;
import java.util.Locale;
import java.util.Map;

final class QqGroupLiveSmokePlan {

    static final String CONFIRMATION = "SEND_TO_TEST_GROUP";

    private QqGroupLiveSmokePlan() {
    }

    static Plan from(Map<String, String> environment) {
        requireEquals(environment.get("QQ_GROUP_LIVE_CONFIRM"), CONFIRMATION,
                "QQ_GROUP_LIVE_CONFIRM must equal " + CONFIRMATION);
        Mode mode = parseMode(required(environment, "QQ_GROUP_LIVE_MODE"));
        String groupOpenId = required(environment, "QQ_GROUP_LIVE_OPENID");
        String messageId = null;
        String eventId = null;
        String imageUrl = null;
        String audioUrl = null;

        if (mode.requiresMessageId()) {
            messageId = required(environment, "QQ_GROUP_LIVE_MSG_ID");
        }
        if (mode == Mode.EVENT) {
            eventId = required(environment, "QQ_GROUP_LIVE_EVENT_ID");
        }
        if (mode == Mode.IMAGE) {
            imageUrl = requireHttpsUrl(environment, "QQ_GROUP_LIVE_IMAGE_URL");
        }
        if (mode == Mode.AUDIO) {
            audioUrl = requireHttpsUrl(environment, "QQ_GROUP_LIVE_AUDIO_URL");
        }
        return new Plan(mode, groupOpenId, messageId, eventId, imageUrl, audioUrl);
    }

    private static Mode parseMode(String value) {
        try {
            return Mode.valueOf(value.toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "QQ_GROUP_LIVE_MODE must be one of REPLY, REFERENCE, EVENT, ACTIVE, IMAGE, AUDIO, MARKDOWN, ARK",
                    exception);
        }
    }

    private static String requireHttpsUrl(Map<String, String> environment, String name) {
        String value = required(environment, name);
        URI uri;
        try {
            uri = URI.create(value);
        }
        catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(name + " must be a valid HTTPS URL", exception);
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw new IllegalArgumentException(name + " must be a valid HTTPS URL");
        }
        return value;
    }

    private static String required(Map<String, String> environment, String name) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required environment variable: " + name);
        }
        return value.trim();
    }

    private static void requireEquals(String actual, String expected, String message) {
        if (!expected.equals(actual)) {
            throw new IllegalArgumentException(message);
        }
    }

    enum Mode {
        REPLY,
        REFERENCE,
        EVENT,
        ACTIVE,
        IMAGE,
        AUDIO,
        MARKDOWN,
        ARK;

        boolean requiresMessageId() {
            return this == REPLY || this == REFERENCE || this == IMAGE
                    || this == AUDIO || this == MARKDOWN || this == ARK;
        }
    }

    record Plan(Mode mode, String groupOpenId, String messageId, String eventId,
                String imageUrl, String audioUrl) {
    }
}
