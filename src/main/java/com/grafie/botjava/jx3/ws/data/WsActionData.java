package com.grafie.botjava.jx3.ws.data;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ws实践的注解
 *
 * @author Grafie
 * @since 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface WsActionData {
    /**
     * Action extends Jx3BaseAction {
    public ActiveCalendarAction(ApiProperties apiProperties, RequestUtl requestUtl) {
        super(apiProperties, requestUtl);
    }

    @Override
    protected RequestResult deal(String requestRegex) {
        return null;
    }
code值
     */
    int actionCode();
}
