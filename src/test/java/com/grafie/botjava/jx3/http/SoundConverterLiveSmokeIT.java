package com.grafie.botjava.jx3.http;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.action.SoundConverterAction;
import com.grafie.botjava.jx3.http.cache.Jx3ApiResponseCache;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.observability.BotMetrics;
import com.grafie.botjava.observability.RequestTraceContext;
import com.grafie.botjava.service.GroupConfigurationService;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Explicitly enabled live acceptance test for the JX3API sound converter. It
 * verifies that a real HTTPS audio URL is generated, but never prints the full
 * URL or credential values.
 */
class SoundConverterLiveSmokeIT {

    @Test
    void shouldGenerateHttpsAudioUrlWithoutPrintingIt() {
        Assumptions.assumeTrue("true".equalsIgnoreCase(environment("JX3_SOUND_LIVE_SMOKE")),
                "Set JX3_SOUND_LIVE_SMOKE=true to run online sound conversion");
        SoundConverterLiveSmokePlan.Plan plan = SoundConverterLiveSmokePlan.from(System.getenv());

        ApiProperties apiProperties = new ApiProperties();
        apiProperties.setApiUrl(plan.apiUrl());
        apiProperties.setApiToken(plan.apiToken());
        apiProperties.setDefaultServer("sound-live-smoke");
        apiProperties.setTicket("sound-live-smoke");
        apiProperties.setName("botjava-jx3");

        Jx3ApiResponseCache cache = mock(Jx3ApiResponseCache.class);
        when(cache.get(anyString(), anyMap())).thenReturn(Optional.empty());
        Jx3RequestUtil requestUtil = new Jx3RequestUtil(
                apiProperties,
                new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false),
                cache,
                mock(BotMetrics.class));
        SoundConverterAction action = new SoundConverterAction(
                apiProperties, requestUtil, mock(GroupConfigurationService.class), plan.soundProperties());

        try (RequestTraceContext.Scope ignored = RequestTraceContext.open("sound-live-smoke")) {
            BotResponse response = action.doRequest(message(),
                    "\u8bed\u97f3 " + plan.text(), REGEX.SoundConverter);

            assertEquals(BotResponse.ResponseType.AUDIO_URL, response.getResponseType());
            URI audioUri = URI.create(response.getAudioUrl());
            assertTrue("https".equalsIgnoreCase(audioUri.getScheme()), "Audio URL must be HTTPS");
            assertTrue(audioUri.getHost() != null && !audioUri.getHost().isBlank(),
                    "Audio URL must include a host");
            System.out.printf("SOUND_CONVERTER\tSUCCESS\thost=%s\turl=redacted%n", audioUri.getHost());
        }
    }

    private static GroupAtMessageCreateDto message() {
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setId("sound-live-smoke-message");
        message.setGroupOpenid("sound-live-smoke-group");
        return message;
    }

    private static String environment(String name) {
        return System.getenv(name);
    }
}
