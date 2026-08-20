package com.grafie.botjava.qq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.util.RemoteHttpException;
import com.grafie.botjava.util.SensitiveDataUtil;

/**
 * 将 QQ HTTP 错误映射为稳定分类和安全提示。
 */
public final class QqOpenApiErrorMapper {

    private QqOpenApiErrorMapper() {
    }

    public static QqOpenApiException fromHttp(RemoteHttpException failure, ObjectMapper objectMapper) {
        ErrorFields fields = parseFields(failure.getResponseBody(), objectMapper);
        int status = failure.getStatusCode();
        QqOpenApiException.Category category = category(status, fields.code());
        return new QqOpenApiException(category, status, fields.code(), safeMessage(fields.message()), fields.traceId(),
                category == QqOpenApiException.Category.RATE_LIMIT || status >= 500,
                userMessage(category), failure);
    }

    public static QqOpenApiException network(Throwable failure) {
        return new QqOpenApiException(QqOpenApiException.Category.NETWORK, 0,
                null, null, null, true, "QQ 服务暂时无法连接，请稍后重试。", failure);
    }

    public static QqOpenApiException invalidResponse(String operation) {
        return new QqOpenApiException(QqOpenApiException.Category.INVALID_RESPONSE, 200,
                null, null, null, false, "QQ 服务返回数据异常，请稍后重试。",
                new IllegalStateException(operation + " 未返回必需字段"));
    }

    private static QqOpenApiException.Category category(int status, String code) {
        if ("100017".equals(code)) {
            return QqOpenApiException.Category.RATE_LIMIT;
        }
        return switch (status) {
            case 400, 405, 409, 422 -> QqOpenApiException.Category.INVALID_REQUEST;
            case 401 -> QqOpenApiException.Category.AUTHENTICATION;
            case 403 -> QqOpenApiException.Category.PERMISSION;
            case 404 -> QqOpenApiException.Category.NOT_FOUND;
            case 429 -> QqOpenApiException.Category.RATE_LIMIT;
            default -> status >= 500
                    ? QqOpenApiException.Category.SERVER_ERROR
                    : QqOpenApiException.Category.UNKNOWN;
        };
    }

    private static String safeMessage(String message) {
        return message == null ? null : SensitiveDataUtil.redactText(message);
    }

    private static String userMessage(QqOpenApiException.Category category) {
        return switch (category) {
            case AUTHENTICATION -> "机器人凭证暂时失效，请联系管理员。";
            case PERMISSION -> "机器人缺少执行该操作的权限。";
            case RATE_LIMIT -> "QQ 接口请求过于频繁，请稍后重试。";
            case INVALID_REQUEST -> "消息内容不符合 QQ 接口要求。";
            case NOT_FOUND -> "QQ 接口目标不存在或已失效。";
            case SERVER_ERROR, NETWORK -> "QQ 服务暂时不可用，请稍后重试。";
            case INVALID_RESPONSE, UNKNOWN -> "QQ 接口返回异常，请稍后重试。";
        };
    }

    private static ErrorFields parseFields(String responseBody, ObjectMapper objectMapper) {
        if (responseBody == null || responseBody.isBlank()) {
            return new ErrorFields(null, null, null);
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            return new ErrorFields(text(root, "code"), firstText(root, "message", "msg"),
                    firstText(root, "trace_id", "traceId"));
        } catch (Exception ignored) {
            return new ErrorFields(null, null, null);
        }
    }

    private static String firstText(JsonNode root, String... names) {
        for (String name : names) {
            String value = text(root, name);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String text(JsonNode root, String name) {
        JsonNode value = root == null ? null : root.get(name);
        if (value == null || value.isNull() || value.isContainerNode()) {
            return null;
        }
        String text = value.asText().trim();
        return text.isEmpty() ? null : text;
    }

    private record ErrorFields(String code, String message, String traceId) {
    }
}
