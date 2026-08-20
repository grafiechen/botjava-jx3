package com.grafie.botjava.jx3.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Api相关配置信息
 *
 * @author Grafie
 * @since 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "jx3api.api")
@Data
@ConditionalOnProperty(prefix = "jx3api", name = {"enabled", "http.enabled"}, havingValue = "true", matchIfMissing = true)
public class ApiProperties {
    /**
     * api访问地址, 如果为空，则默认 https://www.jx3api.com
     */
    private String apiUrl = "https://www.jx3api.com";
    /**
     * api访问token。普通查询和 LV.1 接口使用该 token。
     */
    private String apiToken;
    /**
     * JX3API LV.2 接口 token。为空时回退使用 apiToken，兼容只配置高级 token 的老部署方式。
     */
    private String apiV2Token;
    /**
     * 配置的默认服务器
     */
    private String defaultServer;
    /**
     * ticket
     */
    private String ticket;
    /**
     * 机器人名称
     */
    private String name;
    /**
     * dps计算服务的token
     */
    private String dpsToken;
    /**
     * dps计算服务地址
     */
    private String dpsServiceUrl = "https://www.jx3hps.com";
    /**
     * dps计算服务路径
     */
    private String dpsServicePath = "/dps";
    /**
     * dps计算模型，例如 旗舰、无界。
     */
    private String dpsModel = "旗舰";

    @PostConstruct
    public void validate() {
        Assert.hasText(apiUrl, "jx3api.api.api-url 不能为空");
        Assert.hasText(apiToken, "jx3api.api.api-token 不能为空");
        Assert.hasText(defaultServer, "jx3api.api.default-server 不能为空");
        Assert.hasText(ticket, "jx3api.api.ticket 不能为空");
        Assert.hasText(name, "jx3api.api.name 不能为空");
        Assert.hasText(dpsServiceUrl, "jx3api.api.dps-service-url 不能为空");
        Assert.hasText(dpsServicePath, "jx3api.api.dps-service-path 不能为空");
        Assert.hasText(dpsModel, "jx3api.api.dps-model 不能为空");
    }

    public String resolveToken(int apiLevel) {
        if (apiLevel >= 2 && StringUtils.hasText(apiV2Token)) {
            return apiV2Token;
        }
        return apiToken;
    }
}
