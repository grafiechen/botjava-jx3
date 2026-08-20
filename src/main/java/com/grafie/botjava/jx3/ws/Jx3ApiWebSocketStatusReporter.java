package com.grafie.botjava.jx3.ws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;

/**
 * Reports the effective JX3API WebSocket startup state without exposing credentials.
 */
@Component
@Slf4j
public class Jx3ApiWebSocketStatusReporter {

    private final Environment environment;

    public Jx3ApiWebSocketStatusReporter(Environment environment) {
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void reportStatus() {
        boolean jx3ApiEnabled = environment.getProperty("jx3api.enabled", Boolean.class, true);
        boolean webSocketEnabled = environment.getProperty("jx3api.ws.enabled", Boolean.class, false);
        if (!jx3ApiEnabled) {
            log.info("JX3API WebSocket 未启用，reason=>jx3api.enabled=false");
            return;
        }
        if (!webSocketEnabled) {
            log.info("JX3API WebSocket 未启用，reason=>jx3api.ws.enabled=false");
            return;
        }

        String webSocketUrl = environment.getProperty("jx3api.ws.ws-url");
        String webSocketToken = environment.getProperty("jx3api.ws.ws-token");
        int reconnectDelaySeconds = environment.getProperty(
                "jx3api.ws.re-connect-delay-seconds", Integer.class, 30);
        log.info("JX3API WebSocket 已启用，endpointHost=>{}，tokenConfigured=>{}，reconnectDelaySeconds=>{}",
                endpointHost(webSocketUrl), StringUtils.hasText(webSocketToken), reconnectDelaySeconds);
    }

    private String endpointHost(String webSocketUrl) {
        if (!StringUtils.hasText(webSocketUrl)) {
            return "未配置";
        }
        try {
            String host = URI.create(webSocketUrl).getHost();
            return StringUtils.hasText(host) ? host : "配置格式无效";
        } catch (IllegalArgumentException exception) {
            return "配置格式无效";
        }
    }
}