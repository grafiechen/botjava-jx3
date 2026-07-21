package com.grafie.botjava.jx3.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.action.TradeRecordAction;
import com.grafie.botjava.jx3.http.data.role.RoleDetailedData;
import com.grafie.botjava.jx3.http.data.server.ServerCheckData;
import com.grafie.botjava.jx3.http.data.trade.record.TradeRecordData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.cache.Jx3ApiResponseCache;
import com.grafie.botjava.observability.BotMetrics;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Jx3ApiHttpContractTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final Path CHECK_DOC = Path.of("docs", "testing", "jx3api-http-check.json");

    @Test
    @DisplayName("JX3API_METHOD_PATHS_MATCH_DOCS")
    void shouldMatchOfficialHttpPathsFromCheckDocument() throws Exception {
        Map<String, Object> expected = expected("JX3API_METHOD_PATHS_MATCH_DOCS");

        expected.forEach((enumName, path) -> {
            MethodEnum methodEnum = MethodEnum.valueOf(enumName);
            assertEquals(path, methodEnum.getMethodPath(), enumName);
        });
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("httpContracts")
    @DisplayName("JX3API_ALL_HTTP_DOC_RESULTS_DESERIALIZE")
    void shouldDeserializeEveryOfficialHttpResult(String caseId, CaseData caseData) {
        MethodEnum methodEnum = MethodEnum.valueOf(caseData.raw.get("methodEnum").toString());
        assertEquals(caseData.raw.get("expectedPath"), methodEnum.getMethodPath(), caseId);
        assertNotEquals(Map.class, methodEnum.getResultBeanClass(),
                "Official contract must use a typed DTO: " + caseId);

        BaseResult<?> baseResult = parseResult(caseData, methodEnum);

        assertEquals(200, baseResult.getCode(), caseId);
        assertEquals("success", baseResult.getMsg(), caseId);
        String dataShape = caseData.raw.get("dataShape").toString();
        if ("list".equals(dataShape)) {
            List<?> data = assertInstanceOf(List.class, baseResult.getData(), caseId);
            assertNotNull(data, caseId);
            org.junit.jupiter.api.Assertions.assertFalse(data.isEmpty(), caseId);
            org.junit.jupiter.api.Assertions.assertTrue(
                    data.stream().anyMatch(Jx3ApiHttpContractTest::hasMeaningfulValue),
                    "No non-null value after list deserialization: " + caseId
            );
        } else if ("object".equals(dataShape)) {
            assertNotNull(baseResult.getData(), caseId);
            org.junit.jupiter.api.Assertions.assertTrue(
                    hasMeaningfulValue(baseResult.getData()),
                    "No non-null value after deserialization: " + caseId
            );
        }
    }

    @Test
    @DisplayName("JX3API_STATUS_CHECK_RESULT_JSON")
    void shouldMapStatusCheckResultJson() throws Exception {
        CaseData caseData = caseById("JX3API_STATUS_CHECK_RESULT_JSON");
        BaseResult<?> baseResult = parseResult(caseData, MethodEnum.DATA_SERVER_CHECK);

        ServerCheckData data = assertInstanceOf(ServerCheckData.class, baseResult.getData());
        assertEquals(expectedString(caseData, "zone"), data.getZone());
        assertEquals(expectedString(caseData, "server"), data.getServer());
        assertEquals(expectedString(caseData, "status"), data.getStatus());
    }

    @Test
    @DisplayName("JX3API_TRADE_ITEM_RECORDS_RESULT_JSON")
    void shouldMapTradeItemRecordsResultJson() throws Exception {
        CaseData caseData = caseById("JX3API_TRADE_ITEM_RECORDS_RESULT_JSON");
        BaseResult<?> baseResult = parseResult(caseData, MethodEnum.DATA_TRADE_RECORD);

        TradeRecordData data = assertInstanceOf(TradeRecordData.class, baseResult.getData());
        assertEquals(expectedString(caseData, "classType"), data.getClassType());
        assertEquals(expectedString(caseData, "subclass"), data.getSubclass());
        assertEquals(expectedString(caseData, "name"), data.getName());
        assertNotNull(data.getData());
        assertEquals(expectedString(caseData, "firstRecordServer"), data.getData().get(0).get(0).getServer());
        assertEquals(((Number) caseData.expected().get("firstRecordSale")).intValue(), data.getData().get(0).get(0).getSales());
    }

    @Test
    @DisplayName("JX3API_ROLE_DETAIL_RESULT_JSON")
    void shouldMapRoleDetailResultJson() throws Exception {
        CaseData caseData = caseById("JX3API_ROLE_DETAIL_RESULT_JSON");
        BaseResult<?> baseResult = parseResult(caseData, MethodEnum.DATA_ROLE_DETAILED);

        RoleDetailedData data = assertInstanceOf(RoleDetailedData.class, baseResult.getData());
        assertEquals(expectedString(caseData, "roleName"), data.getRoleName());
        assertEquals(expectedString(caseData, "globalRoleId"), data.getGlobalRoleId());
        assertEquals(expectedString(caseData, "forceName"), data.getForceName());
    }

    @Test
    @DisplayName("JX3API_COMMAND_NORMALIZE_SLASH")
    void shouldNormalizeSlashCommandBeforeRegexMatch() throws Exception {
        CaseData caseData = caseById("JX3API_COMMAND_NORMALIZE_SLASH");
        List<String> inputs = OBJECT_MAPPER.convertValue(caseData.inputObject(), new TypeReference<>() {
        });

        for (String input : inputs) {
            String normalized = REGEX.normalizeCommand(input);
            REGEX regex = REGEX.matchEnum(input);
            Map<String, String> params = regex.handleEncounter(input);

            assertEquals(expectedString(caseData, "normalized"), normalized);
            assertEquals(REGEX.valueOf(expectedString(caseData, "regex")), regex);
            assertEquals(expectedString(caseData, "server"), params.get("server"));
        }
    }

    @Test
    @DisplayName("JX3API_TRADE_RECORD_ACTION_IMAGE_RESPONSE")
    void shouldReturnImageResponseForTradeRecordAction() throws Exception {
        CaseData caseData = caseById("JX3API_TRADE_RECORD_ACTION_IMAGE_RESPONSE");
        Map<String, Object> input = caseData.input();
        Map<String, Object> expected = caseData.expected();

        ApiProperties apiProperties = new ApiProperties();
        apiProperties.setDefaultServer((String) input.get("defaultServer"));
        Jx3RequestUtil jx3RequestUtil = mock(Jx3RequestUtil.class);
        GroupConfigurationService groupConfigurationService = mock(GroupConfigurationService.class);
        when(groupConfigurationService.findServer((String) input.get("groupOpenid")))
                .thenReturn(Optional.of((String) input.get("defaultServer")));

        RequestResult requestResult = new RequestResult();
        requestResult.setCode(200);
        requestResult.setMsg("success");
        TradeRecordData tradeRecordData = new TradeRecordData();
        tradeRecordData.setName((String) expected.get("requestName"));
        BaseResult<TradeRecordData> baseResult = new BaseResult<>();
        baseResult.setCode(200);
        baseResult.setMsg("success");
        baseResult.setData(tradeRecordData);

        when(jx3RequestUtil.doPostRequest(eq(expected.get("requestPath").toString()), org.mockito.ArgumentMatchers.anyMap()))
                .thenReturn(requestResult);
        org.mockito.Mockito.doReturn(baseResult)
                .when(jx3RequestUtil).getResultRealData(requestResult, MethodEnum.DATA_TRADE_RECORD);

        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setGroupOpenid((String) input.get("groupOpenid"));

        TradeRecordAction action = new TradeRecordAction(apiProperties, jx3RequestUtil, groupConfigurationService);
        BotResponse response = action.doRequest(message, (String) input.get("command"), REGEX.TradeRecord);

        assertEquals(BotResponse.ResponseType.valueOf((String) expected.get("responseType")), response.getResponseType());
        assertEquals(expected.get("templateName"), response.getTemplateName());
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(jx3RequestUtil).doPostRequest(eq(expected.get("requestPath").toString()), paramsCaptor.capture());
        assertEquals(expected.get("requestServer"), paramsCaptor.getValue().get("server"));
        assertEquals(expected.get("requestName"), paramsCaptor.getValue().get("name"));
    }

    private BaseResult<?> parseResult(CaseData caseData, MethodEnum methodEnum) {
        ApiProperties apiProperties = new ApiProperties();
        apiProperties.setApiUrl("https://example.invalid");
        apiProperties.setApiToken("TEST_TOKEN");
        Jx3RequestUtil requestUtil = new Jx3RequestUtil(
                apiProperties, OBJECT_MAPPER.copy(),
                mock(Jx3ApiResponseCache.class), mock(BotMetrics.class));
        RequestResult requestResult = OBJECT_MAPPER.convertValue(caseData.input(), RequestResult.class);
        return requestUtil.getResultRealData(requestResult, methodEnum);
    }

    private static String expectedString(CaseData caseData, String key) {
        return caseData.expected().get(key).toString();
    }

    private static Map<String, Object> expected(String caseId) throws Exception {
        return caseById(caseId).expected();
    }

    private static CaseData caseById(String caseId) throws Exception {
        Map<String, Object> document = OBJECT_MAPPER.readValue(CHECK_DOC.toFile(), new TypeReference<>() {
        });
        List<Map<String, Object>> cases = OBJECT_MAPPER.convertValue(document.get("cases"), new TypeReference<>() {
        });
        return cases.stream()
                .filter(item -> caseId.equals(item.get("caseId")))
                .findFirst()
                .map(CaseData::new)
                .orElseThrow(() -> new AssertionError("Missing check case: " + caseId));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> httpContracts() throws Exception {
        Map<String, Object> document = OBJECT_MAPPER.readValue(CHECK_DOC.toFile(), new TypeReference<>() {
        });
        List<Map<String, Object>> contracts = OBJECT_MAPPER.convertValue(document.get("httpContracts"), new TypeReference<>() {
        });
        return contracts.stream()
                .map(CaseData::new)
                .map(caseData -> org.junit.jupiter.params.provider.Arguments.of(caseData.caseId(), caseData));
    }

    private static boolean hasMeaningfulValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof CharSequence text) {
            return !text.toString().isBlank();
        }
        if (value instanceof Number || value instanceof Boolean) {
            return true;
        }
        if (value instanceof List<?> list) {
            return list.stream().anyMatch(Jx3ApiHttpContractTest::hasMeaningfulValue);
        }
        Map<String, Object> data = OBJECT_MAPPER.convertValue(value, new TypeReference<>() {
        });
        return !data.isEmpty() && data.values().stream().anyMatch(Jx3ApiHttpContractTest::hasMeaningfulValue);
    }

    private record CaseData(Map<String, Object> raw) {
        String caseId() {
            return raw.get("caseId").toString();
        }

        Object inputObject() {
            return raw.containsKey("input") ? raw.get("input") : raw.get("result");
        }

        Map<String, Object> input() {
            return OBJECT_MAPPER.convertValue(inputObject(), new TypeReference<>() {
            });
        }

        Map<String, Object> expected() {
            return OBJECT_MAPPER.convertValue(raw.get("expected"), new TypeReference<>() {
            });
        }
    }
}
