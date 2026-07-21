package com.grafie.botjava.jx3.http;

import com.grafie.botjava.jx3.config.SoundProperties;

import java.util.Locale;
import java.util.Map;

final class SoundConverterLiveSmokePlan {

    private static final String DEFAULT_API_URL = "https://www.jx3api.com";
    private static final String DEFAULT_TEXT = "botjava-jx3 sound live smoke";

    private SoundConverterLiveSmokePlan() {
    }

    static Plan from(Map<String, String> environment) {
        requireEquals(environment.get("JX3_SOUND_LIVE_SMOKE"), "true",
                "JX3_SOUND_LIVE_SMOKE must equal true");
        requireEquals(environment.get("JX3_SOUND_ENABLED"), "true",
                "JX3_SOUND_ENABLED must equal true");

        SoundProperties sound = new SoundProperties();
        sound.setEnabled(true);
        sound.setAppkey(required(environment, "JX3_SOUND_APPKEY"));
        sound.setAccess(required(environment, "JX3_SOUND_ACCESS"));
        sound.setSecret(required(environment, "JX3_SOUND_SECRET"));
        sound.setVoice(optional(environment, "JX3_SOUND_VOICE", sound.getVoice()));
        sound.setFormat(optional(environment, "JX3_SOUND_FORMAT", sound.getFormat()).toLowerCase(Locale.ROOT));
        sound.setSampleRate(integer(environment, "JX3_SOUND_SAMPLE_RATE", sound.getSampleRate()));
        sound.setVolume(integer(environment, "JX3_SOUND_VOLUME", sound.getVolume()));
        sound.setSpeechRate(integer(environment, "JX3_SOUND_SPEECH_RATE", sound.getSpeechRate()));
        sound.setPitchRate(integer(environment, "JX3_SOUND_PITCH_RATE", sound.getPitchRate()));
        sound.validate();

        String text = optional(environment, "JX3_SOUND_LIVE_TEXT", DEFAULT_TEXT);
        if (text.length() > 200) {
            throw new IllegalArgumentException("JX3_SOUND_LIVE_TEXT must be at most 200 characters");
        }
        return new Plan(
                required(environment, "JX3API_API_TOKEN"),
                optional(environment, "JX3API_API_URL", DEFAULT_API_URL),
                text,
                sound
        );
    }

    private static int integer(Map<String, String> environment, String name, int fallback) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        }
        catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be an integer", exception);
        }
    }

    private static String optional(Map<String, String> environment, String name, String fallback) {
        String value = environment.get(name);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String required(Map<String, String> environment, String name) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required environment variable: " + name);
        }
        return value.trim();
    }

    private static void requireEquals(String actual, String expected, String message) {
        if (!expected.equalsIgnoreCase(actual == null ? "" : actual.trim())) {
            throw new IllegalArgumentException(message);
        }
    }

    record Plan(String apiToken, String apiUrl, String text, SoundProperties soundProperties) {
    }
}
