package com.grafie.botjava.jx3.http.action.base;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import com.grafie.botjava.observability.RequestTraceContext;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Jx3BaseActionTest {

    @Test
    void shouldKeepRequestContextIsolatedForSingletonAction() throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        ContextAction action = new ContextAction(barrier);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<BotResponse> first = executor.submit(() -> action.doRequest(
                    message("message-1"), "绑定 乾坤一掷", REGEX.BindServerCalendar));
            Future<BotResponse> second = executor.submit(() -> action.doRequest(
                    message("message-2"), "绑定 梦江南", REGEX.BindServerCalendar));

            assertEquals("message-1:绑定 乾坤一掷", first.get().getContent());
            assertEquals("message-2:绑定 梦江南", second.get().getContent());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldKeepReadableFieldsAndDropIdsAndUrls() {
        ContextAction action = new ContextAction(null);
        BaseResult<Map<String, Object>> result = new BaseResult<>();
        result.setData(Map.of(
                "id", 123,
                "url", "https://example.invalid/detail",
                "title", "维护公告",
                "date", "2026-07-11",
                "server", "乾坤一掷"
        ));

        String content = action.format("查询结果", result).getContent();

        assertTrue(content.contains("标题：维护公告"));
        assertTrue(content.contains("日期：2026-07-11"));
        assertTrue(content.contains("服务器：乾坤一掷"));
        assertFalse(content.contains("123"));
        assertFalse(content.contains("https://"));
    }

    @Test
    void shouldHandleEmptyApiDataBeforeCallingSubclass() {
        Jx3RequestUtil requestUtil = mock(Jx3RequestUtil.class);
        RequestResult requestResult = new RequestResult();
        BaseResult<Object> baseResult = new BaseResult<>();
        baseResult.setCode(200);
        baseResult.setData(null);
        when(requestUtil.doPostRequest(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyMap())).thenReturn(requestResult);
        when(requestUtil.getResultRealData(requestResult, REGEX.ServerCheck.getMethodEnum())).thenReturn(baseResult);
        ExternalAction action = new ExternalAction(requestUtil);

        BotResponse response;
        try (RequestTraceContext.Scope ignored = RequestTraceContext.open("trace-command-1")) {
            response = action.doRequest(message("message-3"), "开服", REGEX.ServerCheck);
        }

        assertTrue(response.getContent().startsWith("查询成功，但暂无数据。"));
        assertTrue(response.getContent().contains("请求编号：trace-command-1"));
        assertFalse(RequestTraceContext.currentId().isPresent());
        assertFalse(action.responseBuilt);
        verify(requestUtil).getResultRealData(requestResult, REGEX.ServerCheck.getMethodEnum());
    }

    @Test
    void shouldPreferWholeGroupServerAndFallbackToGlobalDefault() {
        ApiProperties properties = new ApiProperties();
        properties.setDefaultServer("全局默认服");
        GroupConfigurationService configuration = mock(GroupConfigurationService.class);
        when(configuration.findServer("group-1")).thenReturn(Optional.of("群默认服"), Optional.empty());
        DefaultServerAction action = new DefaultServerAction(properties, configuration);

        assertEquals("群默认服", action.doRequest(
                message("message-4"), "绑定 乾坤一掷", REGEX.BindServerCalendar).getContent());
        assertEquals("全局默认服", action.doRequest(
                message("message-5"), "绑定 乾坤一掷", REGEX.BindServerCalendar).getContent());
    }

    private static GroupAtMessageCreateDto message(String id) {
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setId(id);
        message.setGroupOpenid("group-1");
        return message;
    }

    private static class ContextAction extends Jx3BaseAction {
        private final CyclicBarrier barrier;

        ContextAction(CyclicBarrier barrier) {
            super(new ApiProperties(), mock(Jx3RequestUtil.class), mock(GroupConfigurationService.class));
            this.barrier = barrier;
        }

        @Override
        protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
            return Map.of();
        }

        @Override
        protected BotResponse dealAfterJx3ApiRequest(BaseResult baseResult) {
            if (barrier != null) {
                try {
                    barrier.await();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return BotResponse.text(currentMessage().getId() + ":" + currentRequestRegex());
        }

        BotResponse format(String title, BaseResult<?> result) {
            return buildReadableText(title, result);
        }
    }

    private static class ExternalAction extends Jx3BaseAction {
        private boolean responseBuilt;

        ExternalAction(Jx3RequestUtil requestUtil) {
            super(new ApiProperties(), requestUtil, mock(GroupConfigurationService.class));
        }

        @Override
        protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
            return Map.of("server", "乾坤一掷");
        }

        @Override
        protected BotResponse dealAfterJx3ApiRequest(BaseResult baseResult) {
            responseBuilt = true;
            return BotResponse.text("unexpected");
        }
    }

    private static class DefaultServerAction extends Jx3BaseAction {

        DefaultServerAction(ApiProperties properties, GroupConfigurationService configuration) {
            super(properties, mock(Jx3RequestUtil.class), configuration);
        }

        @Override
        protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
            return Map.of();
        }

        @Override
        protected BotResponse dealAfterJx3ApiRequest(BaseResult baseResult) {
            return BotResponse.text(getDefaultServer());
        }
    }
}
