package com.grafie.botjava.jx3.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 启用JX3Api所有模块
 *
 * @author Grafie
 * @since 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@EnableJX3ApiHttp
public @interface EnableJX3Api {

}
