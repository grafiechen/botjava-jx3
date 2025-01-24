package com.grafie.botjava.jx3.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * Api相关配置信息
 *
 * @author Grafie
 * @since 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "jx3api.api")
@Data
@Conditional(OnEnableJX3ApiHttpCondition.class)
public class ApiProperties {
    /**
     * api访问地址, 如果为空，则默认 https://www.jx3api.com
     */
    private String apiUrl = "https://www.jx3api.com";
    /**
     * api访问token，有些api接口，需要校验你的token
     */
    private String apiToken;
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
}
