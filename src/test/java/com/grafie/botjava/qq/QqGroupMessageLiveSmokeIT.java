package com.grafie.botjava.qq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.config.TxBotProperty;
import com.grafie.botjava.entity.dto.common.ArkDto;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.observability.BotMetrics;
import com.grafie.botjava.observability.RequestTraceContext;
import com.grafie.botjava.service.GroupActiveMessagePolicy;
import com.grafie.botjava.service.GroupMessageSender;
import com.grafie.botjava.service.HelpMenuService;
import com.grafie.botjava.service.QqGroupMessageClient;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Explicit, single-mode QQ group delivery acceptance runner. Every execution
 * sends at most one final group message.
 */
class QqGroupMessageLiveSmokeIT {

    @Test
    void shouldSendExactlyOneExplicitlySelectedTestMessage() {
        Assumptions.assumeTrue("true".equalsIgnoreCase(environment("QQ_GROUP_LIVE_SMOKE")),
                "Set QQ_GROUP_LIVE_SMOKE=true to send to a QQ test group");
        QqGroupLiveSmokePlan.Plan plan = QqGroupLiveSmokePlan.from(System.getenv());

        TxBotProperty properties = new TxBotProperty();
        properties.setAppId(requiredEnvironment("TX_BOT_APP_ID"));
        properties.setAppSecret(requiredEnvironment("TX_BOT_APP_SECRET"));
        properties.setAccessTokenUrl(optionalEnvironment(
                "TX_BOT_ACCESS_TOKEN_URL", "https://bots.qq.com/app/getAppAccessToken"));
        properties.setOpenapiUrl(optionalEnvironment(
                "TX_BOT_OPENAPI_URL", "https://api.sgroup.qq.com"));
        properties.setRequestTimeoutSeconds(timeoutSeconds());
        properties.validate();

        QqOpenApiClient openApi = new QqOpenApiClient(properties, new ObjectMapper());
        BotMetrics metrics = mock(BotMetrics.class);
        QqGroupMessageClient client = new QqGroupMessageClient(openApi, metrics);
        GroupActiveMessagePolicy activePolicy = mock(GroupActiveMessagePolicy.class);
        when(activePolicy.acquire(anyString())).thenAnswer(invocation ->
                GroupActiveMessagePolicy.Permit.allowed(
                        invocation.getArgument(0), Instant.now(), "live-smoke-reservation"));
        GroupMessageSender sender = new GroupMessageSender(client, activePolicy);

        String traceId = "qq-live-smoke-" + plan.mode().name().toLowerCase();
        try (RequestTraceContext.Scope ignored = RequestTraceContext.open(traceId)) {
            execute(sender, plan);
        }
        System.out.printf("QQ_GROUP_MESSAGE\t%s\tSUCCESS%n", plan.mode().name());
    }

    private static void execute(GroupMessageSender sender, QqGroupLiveSmokePlan.Plan plan) {
        BotResponse text = BotResponse.text("botjava-jx3 QQ live smoke: " + plan.mode().name());
        switch (plan.mode()) {
            case REPLY -> sender.send(message(plan), text);
            case REFERENCE -> sender.send(message(plan), text.referenceSourceMessage(false));
            case EVENT -> sender.sendEventReply(plan.groupOpenId(), plan.eventId(), text);
            case ACTIVE -> {
                GroupMessageSender.ActiveMessageResult result = sender.sendActive(plan.groupOpenId(), text);
                assertTrue(result.sent(), "Active message was rejected by local policy");
            }
            case IMAGE -> sender.send(message(plan), BotResponse.imageUrl(plan.imageUrl()));
            case AUDIO -> sender.send(message(plan), BotResponse.audioUrl(plan.audioUrl()));
            case MARKDOWN -> sender.send(message(plan), new HelpMenuService(null).interactiveMenu());
            case ARK -> sender.send(message(plan), arkResponse());
        }
    }

    private static BotResponse arkResponse() {
        ArkDto ark = new ArkDto();
        ark.setTemplateId(23);
        ark.setKv(List.of(new ArkDto.KeyValue(
                "#DESC#", "botjava-jx3 QQ live smoke", null)));
        return BotResponse.ark(ark);
    }

    private static GroupAtMessageCreateDto message(QqGroupLiveSmokePlan.Plan plan) {
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setGroupOpenid(plan.groupOpenId());
        message.setId(plan.messageId());
        return message;
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
