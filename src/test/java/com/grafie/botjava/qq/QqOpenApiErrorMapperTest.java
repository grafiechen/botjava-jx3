package com.grafie.botjava.qq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.util.RemoteHttpException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QqOpenApiErrorMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldParseCodeAndTraceWithoutExposingResponseMessage() {
        RemoteHttpException remote = new RemoteHttpException(429,
                "{\"code\":11281,\"message\":\"token=very-secret\",\"trace_id\":\"trace-1\"}", null);

        QqOpenApiException result = QqOpenApiErrorMapper.fromHttp(remote, objectMapper);

        assertEquals(QqOpenApiException.Category.RATE_LIMIT, result.getCategory());
        assertEquals("11281", result.getApiCode());
        assertEquals("trace-1", result.getTraceId());
        assertTrue(result.isRetryable());
        assertFalse(result.getMessage().contains("very-secret"));
        assertFalse(result.getUserMessage().contains("very-secret"));
    }

    @Test
    void shouldClassifyStatusAndTolerateInvalidJson() {
        QqOpenApiException authentication = QqOpenApiErrorMapper.fromHttp(
                new RemoteHttpException(401, "not-json", null), objectMapper);
        QqOpenApiException server = QqOpenApiErrorMapper.fromHttp(
                new RemoteHttpException(503, "", null), objectMapper);

        assertEquals(QqOpenApiException.Category.AUTHENTICATION, authentication.getCategory());
        assertNull(authentication.getApiCode());
        assertFalse(authentication.isRetryable());
        assertEquals(QqOpenApiException.Category.SERVER_ERROR, server.getCategory());
        assertTrue(server.isRetryable());
    }


    @Test
    void shouldClassifyQqBusinessRateLimitCodeFromBadRequest() {
        RemoteHttpException remote = new RemoteHttpException(400,
                "{\"message\":\"接口调用超过频率限制\",\"code\":100017,\"err_code\":40023001,\"trace_id\":\"trace-rate\"}", null);

        QqOpenApiException result = QqOpenApiErrorMapper.fromHttp(remote, objectMapper);

        assertEquals(QqOpenApiException.Category.RATE_LIMIT, result.getCategory());
        assertEquals("100017", result.getApiCode());
        assertEquals("trace-rate", result.getTraceId());
        assertTrue(result.isRetryable());
    }
    @Test
    void shouldCreateTypedNetworkAndInvalidResponseErrors() {
        assertEquals(QqOpenApiException.Category.NETWORK,
                QqOpenApiErrorMapper.network(new RuntimeException("socket failed")).getCategory());
        assertEquals(QqOpenApiException.Category.INVALID_RESPONSE,
                QqOpenApiErrorMapper.invalidResponse("上传图片").getCategory());
    }
}
