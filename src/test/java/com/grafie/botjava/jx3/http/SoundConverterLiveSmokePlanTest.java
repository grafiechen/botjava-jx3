package com.grafie.botjava.jx3.http;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SoundConverterLiveSmokePlanTest {

    @Test
    void shouldRequireExplicitSoundLiveSmokeConfirmation() {
        Map<String, String> values = base();
        values.remove("JX3_SOUND_LIVE_SMOKE");

        assertThrows(IllegalArgumentException.class,
                () -> SoundConverterLiveSmokePlan.from(values));
    }

    @Test
    void shouldBuildEnabledSoundPropertiesFromEnvironment() {
        Map<String, String> values = base();
        values.put("JX3_SOUND_FORMAT", "MP3");
        values.put("JX3_SOUND_SAMPLE_RATE", "8000");
        values.put("JX3_SOUND_VOLUME", "75");
        values.put("JX3_SOUND_SPEECH_RATE", "10");
        values.put("JX3_SOUND_PITCH_RATE", "-10");
        values.put("JX3_SOUND_LIVE_TEXT", "hello");

        SoundConverterLiveSmokePlan.Plan plan = SoundConverterLiveSmokePlan.from(values);

        assertEquals("token-redacted", plan.apiToken());
        assertEquals("https://www.jx3api.com", plan.apiUrl());
        assertEquals("hello", plan.text());
        assertEquals("mp3", plan.soundProperties().getFormat());
        assertEquals(8000, plan.soundProperties().getSampleRate());
        assertEquals(75, plan.soundProperties().getVolume());
        assertEquals(10, plan.soundProperties().getSpeechRate());
        assertEquals(-10, plan.soundProperties().getPitchRate());
    }

    @Test
    void shouldRejectInvalidAudioOptionsAndOverlongText() {
        Map<String, String> invalidRate = base();
        invalidRate.put("JX3_SOUND_SAMPLE_RATE", "44100");
        assertThrows(IllegalArgumentException.class,
                () -> SoundConverterLiveSmokePlan.from(invalidRate));

        Map<String, String> longText = base();
        longText.put("JX3_SOUND_LIVE_TEXT", "x".repeat(201));
        assertThrows(IllegalArgumentException.class,
                () -> SoundConverterLiveSmokePlan.from(longText));
    }

    private static Map<String, String> base() {
        Map<String, String> values = new HashMap<>();
        values.put("JX3_SOUND_LIVE_SMOKE", "true");
        values.put("JX3_SOUND_ENABLED", "true");
        values.put("JX3API_API_TOKEN", "token-redacted");
        values.put("JX3_SOUND_APPKEY", "appkey-redacted");
        values.put("JX3_SOUND_ACCESS", "access-redacted");
        values.put("JX3_SOUND_SECRET", "secret-redacted");
        return values;
    }
}
