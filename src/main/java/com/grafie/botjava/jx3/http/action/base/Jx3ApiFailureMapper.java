package com.grafie.botjava.jx3.http.action.base;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.util.SensitiveDataUtil;

import java.net.SocketTimeoutException;
import java.util.Locale;
import java.util.concurrent.TimeoutException;

final class Jx3ApiFailureMapper {

    private Jx3ApiFailureMapper() {
    }

    static BotResponse fromApiResult(Integer code, String message, String requestId) {
        String normalized = normalize(message);
        if (isRateLimited(code, normalized)) {
            return response("查询过于频繁，请稍后再试。", requestId);
        }
        if (isCredentialFailure(code, normalized)) {
            return response("查询服务认证失败，请联系管理员检查配置。", requestId);
        }
        String upstreamMessage = userFacingUpstreamMessage(code, message, normalized);
        if (!upstreamMessage.isBlank()) {
            return response(upstreamMessage, requestId);
        }
        return response("查询失败，请稍后再试。", requestId);
    }

    static BotResponse fromException(Throwable throwable, String requestId) {
        if (hasCause(throwable, TimeoutException.class) || hasCause(throwable, SocketTimeoutException.class)
                || normalize(throwable == null ? null : throwable.getMessage()).contains("timeout")) {
            return response("查询超时，请稍后再试。", requestId);
        }
        return response("查询失败，请稍后再试。", requestId);
    }

    static BotResponse empty(String requestId) {
        return response("查询成功，但暂无数据。", requestId);
    }

    private static String userFacingUpstreamMessage(Integer code, String rawMessage, String normalizedMessage) {
        if (!isUserCorrectableFailure(code, normalizedMessage)) {
            return "";
        }
        String message = SensitiveDataUtil.redactText(rawMessage == null ? "" : rawMessage).trim()
                .replace('\r', ' ')
                .replace('\n', ' ');
        if (message.length() > 120) {
            return message.substring(0, 117) + "...";
        }
        return message;
    }

    private static boolean isUserCorrectableFailure(Integer code, String message) {
        return Integer.valueOf(400).equals(code)
                || Integer.valueOf(404).equals(code)
                || containsAny(message, "没有", "木有", "未找到", "不存在", "暂无", "为空", "换个名字", "参数");
    }

    private static boolean isRateLimited(Integer code, String message) {
        return Integer.valueOf(429).equals(code)
                || containsAny(message, "限流", "频繁", "次数", "rate limit", "too many requests");
    }

    private static boolean isCredentialFailure(Integer code, String message) {
        return Integer.valueOf(401).equals(code) || Integer.valueOf(403).equals(code)
                || containsAny(message, "token", "ticket", "认证", "凭证", "无权限", "未授权");
    }

    private static boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasCause(Throwable throwable, Class<? extends Throwable> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static BotResponse response(String message, String requestId) {
        return BotResponse.text(message + "（请求编号：" + requestId + "）");
    }
}
