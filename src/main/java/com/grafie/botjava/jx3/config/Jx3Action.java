package com.grafie.botjava.jx3.config;


import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自动托管给spring。
 * 用于某些action需要使用上下文的情况。
 *
 * @author grafie.chen
 * @since 2025/1/22  15:44
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
@ConditionalOnProperty(prefix = "jx3api", name = {"enabled", "http.enabled"}, havingValue = "true", matchIfMissing = true)
public @interface Jx3Action{

}
