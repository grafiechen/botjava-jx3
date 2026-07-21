package com.grafie.botjava.jx3.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import java.util.LinkedHashSet;
import java.util.Set;

@Configuration
@ConfigurationProperties(prefix = "jx3api.remote-image")
@Conditional(OnEnableJX3ApiHttpCondition.class)
@Data
public class RemoteImageProperties {

    private boolean enabled = true;
    private Set<String> allowedHosts = new LinkedHashSet<>(Set.of(
            "www.jx3api.com", "jx3api.com", "nico.nicemoe.cn"));
    private int timeoutSeconds = 5;
    private int maxBytes = 5 * 1024 * 1024;
    private long maxPixels = 16_777_216L;

    @PostConstruct
    public void validate() {
        Assert.notEmpty(allowedHosts, "jx3api.remote-image.allowed-hosts 不能为空");
        Assert.isTrue(allowedHosts.stream().allMatch(this::validHost),
                "jx3api.remote-image.allowed-hosts 只能包含规范域名");
        Assert.isTrue(timeoutSeconds >= 1 && timeoutSeconds <= 15,
                "jx3api.remote-image.timeout-seconds 必须在 1 到 15 之间");
        Assert.isTrue(maxBytes >= 1024 && maxBytes <= 10 * 1024 * 1024,
                "jx3api.remote-image.max-bytes 必须在 1KB 到 10MB 之间");
        Assert.isTrue(maxPixels >= 1 && maxPixels <= 33_554_432L,
                "jx3api.remote-image.max-pixels 必须在 1 到 33554432 之间");
    }

    private boolean validHost(String host) {
        return host != null && host.matches("(?i)^[a-z0-9](?:[a-z0-9.-]*[a-z0-9])?$")
                && !host.contains("..") && !host.contains(":");
    }
}
