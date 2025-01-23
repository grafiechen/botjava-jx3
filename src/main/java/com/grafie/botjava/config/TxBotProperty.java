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
     * 请求服务地址
     */
    private String server;
    /**
     * 基础api地址
     */
    private String signBaseUrl;
    /**
     * 请求url
     */
    private String signUrl;
    /**
     * appId
     */
    private String appId;
    /**
     * 密钥
     */
    private String clientSecret;


}
