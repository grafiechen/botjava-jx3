package com.grafie.botjava.config;

import com.grafie.botjava.jx3.http.util.REGEX;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandReleasePropertiesTest {

    @Test
    void shouldAllowOnlyProductionAndSystemByDefault() {
        CommandReleaseProperties properties = new CommandReleaseProperties();

        assertTrue(properties.allows(REGEX.CommandAvailability.SYSTEM));
        assertTrue(properties.allows(REGEX.CommandAvailability.PRODUCTION));
        assertFalse(properties.allows(REGEX.CommandAvailability.EXPERIMENTAL));
        assertFalse(properties.allows(REGEX.CommandAvailability.REVIEW));
    }

    @Test
    void shouldKeepTestAndReviewModesMutuallyExclusive() {
        CommandReleaseProperties properties = new CommandReleaseProperties();
        properties.setRuntimeMode(CommandReleaseProperties.RuntimeMode.TEST);
        assertTrue(properties.allows(REGEX.CommandAvailability.EXPERIMENTAL));
        assertFalse(properties.allows(REGEX.CommandAvailability.REVIEW));

        properties.setRuntimeMode(CommandReleaseProperties.RuntimeMode.REVIEW);
        assertFalse(properties.allows(REGEX.CommandAvailability.EXPERIMENTAL));
        assertTrue(properties.allows(REGEX.CommandAvailability.REVIEW));
    }
}
