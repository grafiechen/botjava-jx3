package com.grafie.botjava.jx3.config;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import java.util.List;

/**
 * ws配置相关类
 *
 * @author Grafie
 * @since 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "jx3api.ws")
@Data
@ConditionalOnExpression("'${jx3api.enabled:true}' == 'true' and '${jx3api.ws.enabled:false}' == 'true'")
public class WebSocketProperties {
    /**
     * wsToken，ws有些接口需要校验你的token
     */
    private String wsToken;
    /**
     * wsUrl
     */
    private String wsUrl;
    /**
     * 重新连接次数.如果为空，则默认5000
     */
    private Integer reConnectMaxTimes = 5000;
    /**
     * 连接失败或断线后的最小重试间隔，禁止低于 30 秒。
     */
    private Integer reConnectDelaySeconds = 30;
    /**
     * ws数据解析默认包地址，可以为空
     */
    private List<String> wsDataBeanBasePackage;

    public void validate() {
        Assert.hasText(wsUrl, "jx3api.ws.ws-url 不能为空，启用 JX3API WS 时必须配置 JX3API_WS_URL");
        Assert.hasText(wsToken, "jx3api.ws.ws-token 不能为空，启用 JX3API WS 时必须配置 JX3API_WS_TOKEN");
        Assert.notNull(reConnectMaxTimes, "jx3api.ws.re-connect-max-times 不能为空");
        Assert.isTrue(reConnectMaxTimes > 0, "jx3api.ws.re-connect-max-times 必须大于 0");
        Assert.notNull(reConnectDelaySeconds, "jx3api.ws.re-connect-delay-seconds 不能为空");
        Assert.isTrue(reConnectDelaySeconds >= 30,
                "jx3api.ws.re-connect-delay-seconds 不能小于 30 秒");
    }
}
