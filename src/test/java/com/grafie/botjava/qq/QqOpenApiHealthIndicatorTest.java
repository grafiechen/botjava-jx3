package com.grafie.botjava.qq;

import com.grafie.botjava.entity.dto.qq.QqBotUserDto;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QqOpenApiHealthIndicatorTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(QqBotIdentityClient.class, () -> mock(QqBotIdentityClient.class))
            .withUserConfiguration(QqOpenApiHealthIndicator.class);

    @Test
    void shouldOnlyRegisterWhenRemoteHealthCheckIsExplicitlyEnabled() {
        contextRunner.run(context -> assertFalse(context.containsBean("qqOpenApiHealthIndicator")));
        contextRunner.withPropertyValues("tx.bot.health-check-enabled=true")
                .run(context -> assertEquals(1,
                        context.getBeansOfType(QqOpenApiHealthIndicator.class).size()));
    }

    @Test
    void shouldReportUpWithoutIdentityDetails() {
        QqBotIdentityClient identity = mock(QqBotIdentityClient.class);
        when(identity.me()).thenReturn(new QqBotUserDto());

        Health health = new QqOpenApiHealthIndicator(identity).health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals(true, health.getDetails().get("authenticated"));
        assertFalse(health.getDetails().containsKey("botId"));
        assertFalse(health.getDetails().containsKey("username"));
    }

    @Test
    void shouldReportSanitizedFailureCategory() {
        QqBotIdentityClient identity = mock(QqBotIdentityClient.class);
        when(identity.me()).thenThrow(QqOpenApiErrorMapper.network(
                new IllegalStateException("token=secret-value")));

        Health health = new QqOpenApiHealthIndicator(identity).health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("NETWORK", health.getDetails().get("category"));
        assertEquals(0, health.getDetails().get("httpStatus"));
        assertFalse(health.getDetails().toString().contains("secret-value"));
    }
}
