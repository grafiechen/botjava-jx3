package com.grafie.botjava.service;

import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.http.util.REGEX;

/**
 * 外观名称配置权限前置，后续可接数据库配置指定群和成员可维护哪些配置。
 */
public interface AppearanceNamePermissionConfiguration {

    boolean canManageAppearanceName(GroupAtMessageCreateDto message, REGEX command);

    /**
     * 外观名称审核策略。当前阶段默认直接通过，后续可改为数据库或后台审核结果。
     */
    default boolean isAppearanceNameReviewPassed(GroupAtMessageCreateDto message, REGEX command) {
        return true;
    }
}
