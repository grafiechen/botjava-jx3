package com.grafie.botjava.service;

import com.grafie.botjava.jx3.http.util.REGEX;

import java.util.Optional;

/**
 * 权限配置的手动 fallback，供隔离测试或非 Spring 场景使用；生产容器不注册该实现。
 */
public class NoopGroupCommandPermissionConfiguration implements GroupCommandPermissionConfiguration {

    @Override
    public boolean isPermissionCommand(REGEX command) {
        return false;
    }

    @Override
    public Optional<Decision> evaluate(String groupOpenId, String memberOpenId, REGEX command) {
        return Optional.empty();
    }
}