package com.grafie.botjava.service;

import com.grafie.botjava.entity.dto.qq.QqGatewayDto;
import com.grafie.botjava.qq.QqOpenApiClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * QQ OpenAPI v2 WebSocket 网关客户端。
 */
@Service
public class QqGatewayClient {

    private static final String GATEWAY_PATH = "/gateway";
    private static final String GATEWAY_BOT_PATH = "/gateway/bot";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final QqOpenApiClient openApiClient;
    private final Clock clock;
    private CachedGateway gatewayCache;
    private CachedGateway gatewayBotCache;

    @Autowired
    public QqGatewayClient(QqOpenApiClient openApiClient) {
        this(openApiClient, Clock.systemDefaultZone());
    }

    QqGatewayClient(QqOpenApiClient openApiClient, Clock clock) {
        this.openApiClient = openApiClient;
        this.clock = clock;
    }

    public synchronized QqGatewayDto getGateway() {
        if (isValid(gatewayCache)) {
            return gatewayCache.gateway();
        }
        QqGatewayDto gateway = openApiClient.get(GATEWAY_PATH, Map.of(), QqGatewayDto.class);
        gatewayCache = new CachedGateway(gateway, expiresAt());
        return gateway;
    }

    public synchronized QqGatewayDto getGatewayBot() {
        if (isValid(gatewayBotCache)) {
            return gatewayBotCache.gateway();
        }
        QqGatewayDto gateway = openApiClient.get(GATEWAY_BOT_PATH, Map.of(), QqGatewayDto.class);
        gatewayBotCache = new CachedGateway(gateway, expiresAt());
        return gateway;
    }

    private boolean isValid(CachedGateway cached) {
        return cached != null && cached.gateway() != null && Instant.now(clock).isBefore(cached.expiresAt());
    }

    private Instant expiresAt() {
        return Instant.now(clock).plus(CACHE_TTL);
    }

    private record CachedGateway(QqGatewayDto gateway, Instant expiresAt) {
    }
}