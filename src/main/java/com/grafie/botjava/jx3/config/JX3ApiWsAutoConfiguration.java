package com.grafie.botjava.jx3.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.jx3.ws.service.WsDataPushService;
import com.grafie.botjava.jx3.ws.WebSocketClientInitializer;
import jakarta.annotation.Resource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * 自动装配类
 *
 * @author Grafie
 * @since 1.0.0
 */
@Configuration
@ConditionalOnExpression("'${jx3api.enabled:true}' == 'true' and '${jx3api.ws.enabled:false}' == 'true'")
public class JX3ApiWsAutoConfiguration {
    /**
     * 默认ws data的包路径
     */
    private static final String DEFAULT_WS_DATA_PACKAGE = "com.grafie.botjava.jx3.ws.data";
    @Resource
    private WebSocketProperties webSocketProperties;
    @Resource
    private WsDataPushService wsDataPushService;
    @Resource
    private ObjectMapper objectMapper;

    @Bean
    public WebSocketClientInitializer webSocketClientInitializer() throws ClassNotFoundException,
            InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        webSocketProperties.validate();
        if (webSocketProperties.getWsDataBeanBasePackage() == null) {
            List<String> wsDataBasePackageList = new ArrayList<>();
            webSocketProperties.setWsDataBeanBasePackage(wsDataBasePackageList);
        }
        webSocketProperties.getWsDataBeanBasePackage().add(DEFAULT_WS_DATA_PACKAGE);
        return new WebSocketClientInitializer(webSocketProperties, wsDataPushService, objectMapper);
    }

}
