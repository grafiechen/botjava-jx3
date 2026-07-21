package com.grafie.botjava.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

/**
 * @author grafie.chen
 * @since 2025/1/23  9:41
 */
@Configuration
@ConfigurationProperties(prefix = "tx.bot")
@Data
public class TxBotProperty {
    /**
     * openapi 开放接口
     */
    private String openapiUrl;
    /**
     * 获取token接口
     */
    private String accessTokenUrl;
    /**
     * appId/机器人ID
     */
    private String appId;
    /**
     * 机器人密钥
     */
    private String appSecret;
    /**
     * 机器人令牌
     */
    private String token;
    /**
     * 机器人QQ号
     */
    private String number;
    /**
     * 是否通过 Actuator 主动请求 QQ /users/@me 检查远程鉴权与连通性。
     */
    private boolean healthCheckEnabled;
    /**
     * QQ token 与 OpenAPI 请求超时秒数。
     */
    private int requestTimeoutSeconds = 10;

    @PostConstruct
    public void validate() {
        Assert.hasText(openapiUrl, "tx.bot.openapi-url 不能为空");
        Assert.hasText(accessTokenUrl, "tx.bot.access-token-url 不能为空");
        Assert.hasText(appId, "tx.bot.app-id 不能为空");
        Assert.hasText(appSecret, "tx.bot.app-secret 不能为空");
        Assert.isTrue(requestTimeoutSeconds >= 1 && requestTimeoutSeconds <= 60,
                "tx.bot.request-timeout-seconds 必须在 1 到 60 之间");
    }

}
