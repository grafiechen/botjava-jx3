package com.grafie.botjava.qq;

/**
 * QQ OpenAPI 类型化异常，不包含上游响应原文。
 */
public class QqOpenApiException extends RuntimeException {

    private final Category category;
    private final int httpStatus;
    private final String apiCode;
    private final String apiMessage;
    private final String traceId;
    private final boolean retryable;
    private final String userMessage;

    public QqOpenApiException(Category category, int httpStatus, String apiCode, String apiMessage, String traceId,
                              boolean retryable, String userMessage, Throwable cause) {
        super(buildMessage(category, httpStatus, apiCode, apiMessage, traceId), cause);
        this.category = category;
        this.httpStatus = httpStatus;
        this.apiCode = apiCode;
        this.apiMessage = apiMessage;
        this.traceId = traceId;
        this.retryable = retryable;
        this.userMessage = userMessage;
    }

    private static String buildMessage(Category category, int httpStatus, String apiCode,
                                       String apiMessage, String traceId) {
        return "QQ OpenAPI 请求失败，category=" + category
                + "，httpStatus=" + httpStatus
                + "，apiCode=" + (apiCode == null ? "unknown" : apiCode)
                + "，apiMessage=" + (apiMessage == null ? "unknown" : apiMessage)
                + "，traceId=" + (traceId == null ? "unknown" : traceId);
    }

    public Category getCategory() {
        return category;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getApiCode() {
        return apiCode;
    }

    public String getApiMessage() {
        return apiMessage;
    }

    public String getTraceId() {
        return traceId;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public enum Category {
        AUTHENTICATION,
        PERMISSION,
        RATE_LIMIT,
        INVALID_REQUEST,
        NOT_FOUND,
        SERVER_ERROR,
        NETWORK,
        INVALID_RESPONSE,
        UNKNOWN
    }
}
