package com.grafie.botjava.qq;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QqGroupLiveSmokePlanTest {

    @Test
    void shouldRequireStrongConfirmationAndExactlyOneMode() {
        Map<String, String> missingConfirmation = base("ACTIVE");
        missingConfirmation.remove("QQ_GROUP_LIVE_CONFIRM");
        assertThrows(IllegalArgumentException.class,
                () -> QqGroupLiveSmokePlan.from(missingConfirmation));

        Map<String, String> unknownMode = base("UNKNOWN");
        assertThrows(IllegalArgumentException.class,
                () -> QqGroupLiveSmokePlan.from(unknownMode));
    }

    @Test
    void shouldRequireOnlyTheIdentifiersUsedBySelectedMode() {
        QqGroupLiveSmokePlan.Plan active = QqGroupLiveSmokePlan.from(base("ACTIVE"));
        assertNull(active.messageId());
        assertNull(active.eventId());

        Map<String, String> replyValues = base("REPLY");
        assertThrows(IllegalArgumentException.class, () -> QqGroupLiveSmokePlan.from(replyValues));
        replyValues.put("QQ_GROUP_LIVE_MSG_ID", "message-1");
        assertEquals("message-1", QqGroupLiveSmokePlan.from(replyValues).messageId());

        Map<String, String> eventValues = base("EVENT");
        assertThrows(IllegalArgumentException.class, () -> QqGroupLiveSmokePlan.from(eventValues));
        eventValues.put("QQ_GROUP_LIVE_EVENT_ID", "event-1");
        assertEquals("event-1", QqGroupLiveSmokePlan.from(eventValues).eventId());
    }

    @Test
    void shouldOnlyAcceptHttpsImageUrls() {
        Map<String, String> values = base("IMAGE");
        values.put("QQ_GROUP_LIVE_MSG_ID", "message-1");
        values.put("QQ_GROUP_LIVE_IMAGE_URL", "http://media.example.com/test.png");
        assertThrows(IllegalArgumentException.class, () -> QqGroupLiveSmokePlan.from(values));

        values.put("QQ_GROUP_LIVE_IMAGE_URL", "https://media.example.com/test.png");
        assertEquals("https://media.example.com/test.png",
                QqGroupLiveSmokePlan.from(values).imageUrl());
    }

    @Test
    void shouldOnlyAcceptHttpsAudioUrls() {
        Map<String, String> missingMessage = base("AUDIO");
        missingMessage.put("QQ_GROUP_LIVE_AUDIO_URL", "https://media.example.com/test.mp3");
        assertThrows(IllegalArgumentException.class, () -> QqGroupLiveSmokePlan.from(missingMessage));

        Map<String, String> values = base("AUDIO");
        values.put("QQ_GROUP_LIVE_MSG_ID", "message-1");
        values.put("QQ_GROUP_LIVE_AUDIO_URL", "http://media.example.com/test.mp3");
        assertThrows(IllegalArgumentException.class, () -> QqGroupLiveSmokePlan.from(values));

        values.put("QQ_GROUP_LIVE_AUDIO_URL", "https://media.example.com/test.mp3");
        assertEquals("https://media.example.com/test.mp3",
                QqGroupLiveSmokePlan.from(values).audioUrl());
    }

    private static Map<String, String> base(String mode) {
        Map<String, String> values = new HashMap<>();
        values.put("QQ_GROUP_LIVE_CONFIRM", QqGroupLiveSmokePlan.CONFIRMATION);
        values.put("QQ_GROUP_LIVE_MODE", mode);
        values.put("QQ_GROUP_LIVE_OPENID", "test-group");
        return values;
    }
}
