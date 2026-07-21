package com.grafie.botjava.util;

/**
 * 远程 HTTP 非 2xx 响应。异常消息不包含响应体，避免上游敏感内容进入日志。
 */
public class RemoteHttpException extends RuntimeException {

    private final int statusCode;
    private final String responseBody;

    public RemoteHttpException(int statusCode, String responseBody, Throwable cause) {
        super("远程 HTTP 请求失败，status=" + statusCode, cause);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
