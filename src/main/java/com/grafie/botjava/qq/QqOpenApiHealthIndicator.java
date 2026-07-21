package com.grafie.botjava.qq;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 可选 QQ OpenAPI 远程健康检查，不暴露机器人标识或凭证。
 */
@Component("qqOpenApiHealthIndicator")
@ConditionalOnProperty(prefix = "tx.bot", name = "health-check-enabled", havingValue = "true")
public class QqOpenApiHealthIndicator implements HealthIndicator {

    private final QqBotIdentityClient identityClient;

    public QqOpenApiHealthIndicator(QqBotIdentityClient identityClient) {
        this.identityClient = identityClient;
    }

    @Override
    public Health health() {
        try {
            identityClient.me();
            return Health.up().withDetail("authenticated", true).build();
        } catch (QqOpenApiException failure) {
            return Health.down()
                    .withDetail("category", failure.getCategory().name())
                    .withDetail("httpStatus", failure.getHttpStatus())
                    .build();
        } catch (RuntimeException failure) {
            return Health.down().withDetail("category", "UNEXPECTED").build();
        }
    }
}
