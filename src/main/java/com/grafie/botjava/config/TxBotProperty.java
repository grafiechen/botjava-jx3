package com.grafie.botjava.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

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

}
