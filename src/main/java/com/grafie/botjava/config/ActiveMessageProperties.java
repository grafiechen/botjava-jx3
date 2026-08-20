package com.grafie.botjava.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "bot.qq")
public class ActiveMessageProperties {

    private int activeMessageMinIntervalSeconds = 60;
    private boolean verifyPlatformStateBeforeActiveSend = false;

    public int getActiveMessageMinIntervalSeconds() {
        return activeMessageMinIntervalSeconds;
    }

    public void setActiveMessageMinIntervalSeconds(int activeMessageMinIntervalSeconds) {
        if (activeMessageMinIntervalSeconds < 1) {
            throw new IllegalArgumentException("bot.qq.active-message-min-interval-seconds 必须大于 0");
        }
        this.activeMessageMinIntervalSeconds = activeMessageMinIntervalSeconds;
    }

    public boolean isVerifyPlatformStateBeforeActiveSend() {
        return verifyPlatformStateBeforeActiveSend;
    }

    public void setVerifyPlatformStateBeforeActiveSend(boolean verifyPlatformStateBeforeActiveSend) {
        this.verifyPlatformStateBeforeActiveSend = verifyPlatformStateBeforeActiveSend;
    }
}
