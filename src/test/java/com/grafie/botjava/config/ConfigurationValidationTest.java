package com.grafie.botjava.config;

import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.jx3.config.RemoteImageProperties;
import com.grafie.botjava.jx3.config.WebSocketProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;

class ConfigurationValidationTest {

    @Test
    void shouldRejectIncompleteQqConfiguration() {
        TxBotProperty properties = new TxBotProperty();

        assertThrows(IllegalArgumentException.class, properties::validate);
    }

    @Test
    void shouldAcceptRequiredQqConfiguration() {
        TxBotProperty properties = new TxBotProperty();
        properties.setOpenapiUrl("https://api.bot.qq.com");
        properties.setAccessTokenUrl("https://api.bot.qq.com/app/getAppAccessToken");
        properties.setAppId("app-id");
        properties.setAppSecret("app-secret");

        assertDoesNotThrow(properties::validate);
        assertEquals(10, properties.getRequestTimeoutSeconds());
    }

    @Test
    void shouldValidateQqRequestTimeoutAndOptionalHealthCheck() {
        TxBotProperty tooShort = validQqProperties();
        tooShort.setRequestTimeoutSeconds(0);
        TxBotProperty tooLong = validQqProperties();
        tooLong.setRequestTimeoutSeconds(61);
        TxBotProperty enabled = validQqProperties();
        enabled.setHealthCheckEnabled(true);

        assertThrows(IllegalArgumentException.class, tooShort::validate);
        assertThrows(IllegalArgumentException.class, tooLong::validate);
        assertDoesNotThrow(enabled::validate);
        assertEquals(true, enabled.isHealthCheckEnabled());
    }

    @Test
    void shouldRejectEnabledJx3ApiWithoutToken() {
        ApiProperties properties = new ApiProperties();

        assertThrows(IllegalArgumentException.class, properties::validate);
    }

    @Test
    void shouldAcceptRequiredJx3ApiConfiguration() {
        ApiProperties properties = validJx3Properties();

        assertDoesNotThrow(properties::validate);
        assertEquals("https://www.jx3hps.com", properties.getDpsServiceUrl());
        assertEquals("/dps", properties.getDpsServicePath());
        assertEquals("旗舰", properties.getDpsModel());
    }

    @Test
    void shouldValidateJx3ApiCoreConfiguration() {
        ApiProperties missingDefaultServer = validJx3Properties();
        missingDefaultServer.setDefaultServer("");
        ApiProperties missingTicket = validJx3Properties();
        missingTicket.setTicket("");
        ApiProperties missingName = validJx3Properties();
        missingName.setName("");

        assertThrows(IllegalArgumentException.class, missingDefaultServer::validate);
        assertThrows(IllegalArgumentException.class, missingTicket::validate);
        assertThrows(IllegalArgumentException.class, missingName::validate);
    }

    @Test
    void shouldValidateDpsServiceConfiguration() {
        ApiProperties missingUrl = validJx3Properties();
        missingUrl.setDpsServiceUrl("");
        ApiProperties missingPath = validJx3Properties();
        missingPath.setDpsServicePath("");
        ApiProperties missingModel = validJx3Properties();
        missingModel.setDpsModel("");

        assertThrows(IllegalArgumentException.class, missingUrl::validate);
        assertThrows(IllegalArgumentException.class, missingPath::validate);
        assertThrows(IllegalArgumentException.class, missingModel::validate);
    }

    @Test
    void shouldValidateRemoteImageSafetyLimits() {
        RemoteImageProperties valid = new RemoteImageProperties();
        RemoteImageProperties invalidHost = new RemoteImageProperties();
        invalidHost.setAllowedHosts(Set.of("https://www.jx3api.com"));
        RemoteImageProperties invalidSize = new RemoteImageProperties();
        invalidSize.setMaxBytes(512);
        RemoteImageProperties invalidTimeout = new RemoteImageProperties();
        invalidTimeout.setTimeoutSeconds(16);

        assertDoesNotThrow(valid::validate);
        assertThrows(IllegalArgumentException.class, invalidHost::validate);
        assertThrows(IllegalArgumentException.class, invalidSize::validate);
        assertThrows(IllegalArgumentException.class, invalidTimeout::validate);
    }

    @Test
    void shouldControlJx3ApiHttpActionRegistrationFromProperties() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withUserConfiguration(ConditionalJx3ActionMarker.class);

        runner.run(context ->
                assertFalse(context.getBeansOfType(ConditionalJx3ActionMarker.class).isEmpty()));
        runner.withPropertyValues("jx3api.enabled=false")
                .run(context ->
                        assertTrue(context.getBeansOfType(ConditionalJx3ActionMarker.class).isEmpty()));
        runner.withPropertyValues("jx3api.http.enabled=false")
                .run(context ->
                        assertTrue(context.getBeansOfType(ConditionalJx3ActionMarker.class).isEmpty()));
    }
    @Test
    void shouldControlJx3ApiWebSocketPushFromProperties() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withUserConfiguration(WebSocketProperties.class);

        runner.run(context -> assertFalse(context.containsBean("webSocketProperties")));
        runner.withPropertyValues("jx3api.ws.enabled=true")
                .run(context -> assertTrue(context.containsBean("webSocketProperties")));
        runner.withPropertyValues("jx3api.enabled=false", "jx3api.ws.enabled=true")
                .run(context -> assertFalse(context.containsBean("webSocketProperties")));
    }
    @Test
    void shouldValidateAndResolvePerCommandCooldown() {
        CommandCooldownProperties properties = new CommandCooldownProperties();
        properties.setCooldownSeconds(Map.of("servercheck", 9));

        assertDoesNotThrow(properties::validate);
        assertEquals(9, properties.getCooldown(com.grafie.botjava.jx3.http.util.REGEX.ServerCheck).toSeconds());
    }

    @Test
    void shouldRejectUnknownOrInvalidCommandCooldown() {
        CommandCooldownProperties unknown = new CommandCooldownProperties();
        unknown.setCooldownSeconds(Map.of("UnknownCommand", 10));
        CommandCooldownProperties invalid = new CommandCooldownProperties();
        invalid.setCooldownSeconds(Map.of("ServerCheck", -1));

        assertThrows(IllegalArgumentException.class, unknown::validate);
        assertThrows(IllegalArgumentException.class, invalid::validate);
    }

    @Test
    void shouldValidateCooldownInFlightTimeout() {
        CommandCooldownProperties tooShort = new CommandCooldownProperties();
        tooShort.setCooldownInFlightTimeoutSeconds(29);
        CommandCooldownProperties tooLong = new CommandCooldownProperties();
        tooLong.setCooldownInFlightTimeoutSeconds(3601);

        assertThrows(IllegalArgumentException.class, tooShort::validate);
        assertThrows(IllegalArgumentException.class, tooLong::validate);
    }

    @Test
    void shouldDefaultQqIngressToWebSocketAndAllowWebhookAlias() {
        QqIngressProperties properties = new QqIngressProperties();

        assertEquals(QqIngressProperties.MessageIngressMode.WS, properties.getMessageIngressMode());
        assertEquals(true, properties.isWebSocketEnabled());
        assertEquals(false, properties.isWebhookEnabled());

        properties.setMessageIngressMode(QqIngressProperties.MessageIngressMode.HOOK);
        assertEquals(false, properties.isWebSocketEnabled());
        assertEquals(true, properties.isWebhookEnabled());

        properties.setMessageIngressMode(QqIngressProperties.MessageIngressMode.WEBHOOK);
        assertEquals(true, properties.isWebhookEnabled());
    }

    @Test
    void shouldValidateQqWebSocketProperties() {
        QqWebSocketProperties properties = new QqWebSocketProperties();
        assertEquals(33554432, properties.getIntents());
        assertEquals(0, properties.getShardId());
        assertEquals(1, properties.getShardCount());
        assertEquals(true, properties.isUseGatewayBot());

        assertThrows(IllegalArgumentException.class, () -> properties.setIntents(0));
        assertThrows(IllegalArgumentException.class, () -> properties.setShardId(-1));
        assertThrows(IllegalArgumentException.class, () -> properties.setShardCount(0));
        assertThrows(IllegalArgumentException.class, () -> properties.setReconnectDelaySeconds(0));
        assertThrows(IllegalArgumentException.class, () -> properties.setHandshakeCheckSeconds(0));
    }
    @Jx3Action
    private static class ConditionalJx3ActionMarker {
    }

    private static TxBotProperty validQqProperties() {
        TxBotProperty properties = new TxBotProperty();
        properties.setOpenapiUrl("https://api.bot.qq.com");
        properties.setAccessTokenUrl("https://api.bot.qq.com/app/getAppAccessToken");
        properties.setAppId("app-id");
        properties.setAppSecret("app-secret");
        return properties;
    }

    private static ApiProperties validJx3Properties() {
        ApiProperties properties = new ApiProperties();
        properties.setApiToken("api-token");
        properties.setTicket("ticket");
        properties.setDefaultServer("乾坤一掷");
        properties.setName("botjava-jx3");
        return properties;
    }
}
