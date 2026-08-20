package com.grafie.botjava.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * QQ 消息入口模式配置。
 */
@Component
@ConfigurationProperties(prefix = "bot.qq")
public class QqIngressProperties {

    private MessageIngressMode messageIngressMode = MessageIngressMode.WS;
    private long webhookMaxSignatureAgeSeconds = 300;
    private int webhookMaxBodyBytes = 1_048_576;

    public MessageIngressMode getMessageIngressMode() {
        return messageIngressMode;
    }

    public void setMessageIngressMode(MessageIngressMode messageIngressMode) {
        if (messageIngressMode == null) {
            throw new IllegalArgumentException("bot.qq.message-ingress-mode 不能为空");
        }
        this.messageIngressMode = messageIngressMode;
    }

    public long getWebhookMaxSignatureAgeSeconds() {
        return webhookMaxSignatureAgeSeconds;
    }

    public void setWebhookMaxSignatureAgeSeconds(long webhookMaxSignatureAgeSeconds) {
        if (webhookMaxSignatureAgeSeconds < 30 || webhookMaxSignatureAgeSeconds > 3600) {
            throw new IllegalArgumentException("bot.qq.webhook-max-signature-age-seconds 必须在 30 到 3600 之间");
        }
        this.webhookMaxSignatureAgeSeconds = webhookMaxSignatureAgeSeconds;
    }

    public int getWebhookMaxBodyBytes() {
        return webhookMaxBodyBytes;
    }

    public void setWebhookMaxBodyBytes(int webhookMaxBodyBytes) {
        if (webhookMaxBodyBytes < 1024 || webhookMaxBodyBytes > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("bot.qq.webhook-max-body-bytes 必须在 1024 到 5242880 之间");
        }
        this.webhookMaxBodyBytes = webhookMaxBodyBytes;
    }

    public boolean isWebSocketEnabled() {
        return messageIngressMode == MessageIngressMode.WS;
    }

    public boolean isWebhookEnabled() {
        return messageIngressMode == MessageIngressMode.HOOK
                || messageIngressMode == MessageIngressMode.WEBHOOK;
    }

    public enum MessageIngressMode {
        WS,
        HOOK,
        WEBHOOK
    }
}
