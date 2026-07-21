package com.grafie.botjava.jx3.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SoundPropertiesTest {

    @Test
    void shouldAllowDisabledSoundWithoutCredentials() {
        assertDoesNotThrow(new SoundProperties()::validate);
    }

    @Test
    void shouldValidateEnabledCredentialsAndAudioOptions() {
        SoundProperties valid = enabledProperties();
        assertDoesNotThrow(valid::validate);

        SoundProperties missingSecret = enabledProperties();
        missingSecret.setSecret(null);
        assertThrows(IllegalArgumentException.class, missingSecret::validate);

        SoundProperties invalidSampleRate = enabledProperties();
        invalidSampleRate.setSampleRate(44100);
        assertThrows(IllegalArgumentException.class, invalidSampleRate::validate);
    }

    private static SoundProperties enabledProperties() {
        SoundProperties properties = new SoundProperties();
        properties.setEnabled(true);
        properties.setAppkey("app-key");
        properties.setAccess("access-key");
        properties.setSecret("secret-key");
        return properties;
    }
}
