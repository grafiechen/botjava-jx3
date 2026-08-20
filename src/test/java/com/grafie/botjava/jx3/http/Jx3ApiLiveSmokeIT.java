package com.grafie.botjava.jx3.http;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.cache.Jx3ApiResponseCache;
import com.grafie.botjava.jx3.http.command.CommandArguments;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.observability.BotMetrics;
import com.grafie.botjava.service.GroupConfigurationService;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Explicitly enabled live acceptance test. This class is not included by the
 * default Surefire naming patterns and never runs during ordinary mvn test.
 */
class Jx3ApiLiveSmokeIT {

    @Test
    void shouldCallEveryNonVoiceOfficialContractWithoutLoggingBusinessData() throws Exception {
        Assumptions.assumeTrue("true".equalsIgnoreCase(environment("JX3API_LIVE_SMOKE")),
                "Set JX3API_LIVE_SMOKE=true to run online requests");
        String token = requiredEnvironment("JX3API_API_TOKEN");
        String apiV2Token = optionalEnvironment("JX3API_API_V2_TOKEN", token);
        String ticket = requiredEnvironment("JX3API_TICKET");
        String server = requiredEnvironment("JX3API_SMOKE_SERVER");
        String roleName = requiredEnvironment("JX3API_SMOKE_ROLE");
        String uid = requiredEnvironment("JX3API_SMOKE_UID");
        String apiUrl = optionalEnvironment("JX3API_API_URL", "https://www.jx3api.com");
        long delayMillis = delayMillis();

        ApiProperties properties = new ApiProperties();
        properties.setApiUrl(apiUrl);
        properties.setApiToken(token);
        properties.setApiV2Token(apiV2Token);
        properties.setTicket(ticket);
        properties.setDefaultServer(server);

        Jx3ApiResponseCache cache = mock(Jx3ApiResponseCache.class);
        when(cache.get(anyString(), anyMap())).thenReturn(Optional.empty());
        RecordingRequestUtil requestUtil = new RecordingRequestUtil(
                properties,
                new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false),
                cache,
                mock(BotMetrics.class));
        GroupConfigurationService groupConfigurationService = mock(GroupConfigurationService.class);
        when(groupConfigurationService.findServer("live-smoke-group")).thenReturn(Optional.of(server));

        List<Jx3ApiLiveSmokePlan.SmokeCase> cases = Jx3ApiLiveSmokePlan.build(server, roleName, uid);
        assertEquals(Jx3ApiLiveSmokePlan.officialContractCount() - 1, cases.size());
        List<String> failures = new ArrayList<>();
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setGroupOpenid("live-smoke-group");

        for (int index = 0; index < cases.size(); index++) {
            Jx3ApiLiveSmokePlan.SmokeCase smokeCase = cases.get(index);
            requestUtil.reset();
            Jx3BaseAction action = instantiate(
                    smokeCase.definition().getBaseAction(), properties, requestUtil, groupConfigurationService);
            CommandArguments arguments = CommandArguments.of(
                    smokeCase.definition().handleEncounter(smokeCase.command()));
            BotResponse response = action.doRequest(
                    message, smokeCase.command(), smokeCase.definition(), arguments);

            RequestResult result = requestUtil.lastResult();
            String outcome = outcome(result, requestUtil.lastFailure(), response);
            Integer code = result == null ? null : result.getCode();
            System.out.printf("%s\t%s\t%s\t%s%n",
                    smokeCase.definition().getMethodEnum().name(),
                    smokeCase.definition().getMethodEnum().getMethodPath(),
                    outcome,
                    code == null ? "-" : code);
            if ("FAILED".equals(outcome)) {
                failures.add(smokeCase.definition().getMethodEnum().name()
                        + " [code=" + (code == null ? "-" : code) + "]");
            }
            if (index + 1 < cases.size() && delayMillis > 0) {
                Thread.sleep(delayMillis);
            }
        }

        assertTrue(failures.isEmpty(), "Live smoke failures:\n" + String.join("\n", failures));
    }

    private static String outcome(RequestResult result, Throwable failure, BotResponse response) {
        if (failure != null || result == null || result.getCode() == null || result.getCode() != 200) {
            return "FAILED";
        }
        String content = response == null ? null : response.getContent();
        if (content != null && (content.startsWith("查询失败")
                || content.startsWith("查询超时")
                || content.startsWith("查询服务认证失败")
                || content.startsWith("查询过于频繁"))) {
            return "FAILED";
        }
        return isEmpty(result.getData()) ? "EMPTY" : "SUCCESS";
    }

    private static boolean isEmpty(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof CharSequence sequence) {
            return sequence.toString().isBlank();
        }
        if (value instanceof Collection<?> collection) {
            return collection.isEmpty();
        }
        return value instanceof Map<?, ?> map && map.isEmpty();
    }

    private static Jx3BaseAction instantiate(
            Class<? extends Jx3BaseAction> type,
            ApiProperties properties,
            Jx3RequestUtil requestUtil,
            GroupConfigurationService groupConfigurationService
    ) {
        try {
            @SuppressWarnings("unchecked")
            Constructor<? extends Jx3BaseAction> constructor =
                    (Constructor<? extends Jx3BaseAction>) type.getConstructors()[0];
            Object[] arguments = Arrays.stream(constructor.getParameterTypes())
                    .map(parameterType -> constructorArgument(
                            parameterType, properties, requestUtil, groupConfigurationService))
                    .toArray();
            return constructor.newInstance(arguments);
        }
        catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot construct smoke Action: " + type.getName(), exception);
        }
    }

    private static Object constructorArgument(
            Class<?> type,
            ApiProperties properties,
            Jx3RequestUtil requestUtil,
            GroupConfigurationService groupConfigurationService
    ) {
        if (type == ApiProperties.class) {
            return properties;
        }
        if (type == Jx3RequestUtil.class) {
            return requestUtil;
        }
        if (type == GroupConfigurationService.class) {
            return groupConfigurationService;
        }
        return mock(type);
    }

    private static long delayMillis() {
        String value = optionalEnvironment("JX3API_SMOKE_DELAY_MS", "500");
        try {
            long delay = Long.parseLong(value);
            if (delay < 0 || delay > 10_000) {
                throw new IllegalArgumentException("JX3API_SMOKE_DELAY_MS must be between 0 and 10000");
            }
            return delay;
        }
        catch (NumberFormatException exception) {
            throw new IllegalArgumentException("JX3API_SMOKE_DELAY_MS must be an integer", exception);
        }
    }

    private static String requiredEnvironment(String name) {
        String value = environment(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }
        return value.trim();
    }

    private static String optionalEnvironment(String name, String fallback) {
        String value = environment(name);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String environment(String name) {
        return System.getenv(name);
    }

    private static final class RecordingRequestUtil extends Jx3RequestUtil {
        private RequestResult lastResult;
        private Throwable lastFailure;

        private RecordingRequestUtil(ApiProperties properties, ObjectMapper objectMapper,
                                     Jx3ApiResponseCache cache, BotMetrics metrics) {
            super(properties, objectMapper, cache, metrics);
        }

        @Override
        public RequestResult doPostRequest(String path, Map<String, Object> params) {
            try {
                lastResult = super.doPostRequest(path, params);
                return lastResult;
            }
            catch (RuntimeException exception) {
                lastFailure = exception;
                throw exception;
            }
        }
        @Override
        public RequestResult doGetRequest(MethodEnum methodEnum, Map<String, Object> params) {
            try {
                lastResult = super.doGetRequest(methodEnum, params);
                return lastResult;
            }
            catch (RuntimeException exception) {
                lastFailure = exception;
                throw exception;
            }
        }

        private RequestResult lastResult() {
            return lastResult;
        }

        private Throwable lastFailure() {
            return lastFailure;
        }

        private void reset() {
            lastResult = null;
            lastFailure = null;
        }
    }
}
