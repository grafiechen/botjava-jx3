package com.grafie.botjava.entity;

/**
 * 群指令从匹配到发送结束的最终状态。
 */
public enum CommandInvocationStatus {
    SUCCESS,
    PERMISSION_DENIED,
    FEATURE_DISABLED,
    INVALID_ARGUMENTS,
    COOLDOWN,
    NO_RESPONSE,
    FAILED
}
