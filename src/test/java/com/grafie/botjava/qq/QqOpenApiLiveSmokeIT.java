package com.grafie.botjava.qq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.config.TxBotProperty;
import com.grafie.botjava.entity.dto.qq.QqBotUserDto;
import com.grafie.botjava.observability.BotMetrics;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Side-effect-free QQ authentication probe. Explicit execution is required;
 * the default Maven and CI suites do not include *IT classes.
 */
class QqOpenApiLiveSmokeIT {

    @Test
    void shouldAuthenticateAndLoadCurrentBotIdentity() {
        Assumptions.assumeTrue("true".equalsIgnoreCase(environment("QQ_OPENAPI_LIVE_SMOKE")),
                "Set QQ_OPENAPI_LIVE_SMOKE=true to call QQ OpenAPI");

        TxBotProperty properties = new TxBotProperty();
        properties.setAppId(requiredEnvironment("TX_BOT_APP_ID"));
        properties.setAppSecret(requiredEnvironment("TX_BOT_APP_SECRET"));
        properties.setAccessTokenUrl(optionalEnvironment(
                "TX_BOT_ACCESS_TOKEN_URL", "https://bots.qq.com/app/getAppAccessToken"));
        properties.setOpenapiUrl(optionalEnvironment(
                "TX_BOT_OPENAPI_URL", "https://api.sgroup.qq.com"));
        properties.setRequestTimeoutSeconds(timeoutSeconds());
        properties.validate();

        QqOpenApiClient openApiClient = new QqOpenApiClient(properties, new ObjectMapper());
        QqBotUserDto bot = new QqBotIdentityClient(openApiClient, mock(BotMetrics.class)).me();

        assertTrue(Boolean.TRUE.equals(bot.getBot()));
        System.out.println("QQ_IDENTITY\t/users/@me\tSUCCESS\tbot=true");
    }

    private static int timeoutSeconds() {
        String value = optionalEnvironment("TX_BOT_REQUEST_TIMEOUT_SECONDS", "10");
        try {
            int timeout = Integer.parseInt(value);
            if (timeout < 1 || timeout > 60) {
                throw new IllegalArgumentException(
                        "TX_BOT_REQUEST_TIMEOUT_SECONDS must be between 1 and 60");
            }
            return timeout;
        }
        catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "TX_BOT_REQUEST_TIMEOUT_SECONDS must be an integer", exception);
        }
    }

    private static String requiredEnvironment(String name) {
        String value = environment(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }
        return value.trim();
    }

    private static String optionalEnvironment(String name, String fallback) {
        String value = environment(name);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String environment(String name) {
        return System.getenv(name);
    }
}
