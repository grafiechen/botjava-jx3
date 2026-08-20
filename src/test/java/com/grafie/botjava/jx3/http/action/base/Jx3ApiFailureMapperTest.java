package com.grafie.botjava.jx3.http.action.base;

import org.junit.jupiter.api.Test;

import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Jx3ApiFailureMapperTest {

    @Test
    void shouldMapRateLimitResponses() {
        assertEquals("查询过于频繁，请稍后再试。（请求编号：rate-1）",
                Jx3ApiFailureMapper.fromApiResult(429, "Too Many Requests", "rate-1").getContent());
        assertEquals("查询过于频繁，请稍后再试。（请求编号：rate-2）",
                Jx3ApiFailureMapper.fromApiResult(500, "今日调用次数已达上限", "rate-2").getContent());
    }

    @Test
    void shouldMapCredentialResponsesWithoutExposingUpstreamMessage() {
        assertEquals("查询服务认证失败，请联系管理员检查配置。（请求编号：auth-1）",
                Jx3ApiFailureMapper.fromApiResult(401, "token=secret-token 已失效", "auth-1").getContent());
    }


    @Test
    void shouldReturnUserCorrectableUpstreamMessage() {
        assertEquals("木有找到相关外观，换个名字试试呢（请求编号：not-found-1）",
                Jx3ApiFailureMapper.fromApiResult(404, "木有找到相关外观，换个名字试试呢", "not-found-1").getContent());
        assertEquals("参数错误：缺少 server（请求编号：bad-request-1）",
                Jx3ApiFailureMapper.fromApiResult(400, "参数错误：缺少 server", "bad-request-1").getContent());
    }
    @Test
    void shouldMapTimeoutAndGenericFailures() {
        assertEquals("查询超时，请稍后再试。（请求编号：timeout-1）",
                Jx3ApiFailureMapper.fromException(
                        new RuntimeException(new SocketTimeoutException("read timeout")), "timeout-1").getContent());
        assertEquals("查询失败，请稍后再试。（请求编号：error-1）",
                Jx3ApiFailureMapper.fromException(new IllegalStateException("broken"), "error-1").getContent());
    }
}
