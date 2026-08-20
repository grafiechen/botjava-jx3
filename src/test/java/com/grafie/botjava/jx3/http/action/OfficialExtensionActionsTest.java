package com.grafie.botjava.jx3.http.action;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.MethodEnum;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.data.official.ChatRecordsData;
import com.grafie.botjava.jx3.http.data.official.FlexibleOfficialData;
import com.grafie.botjava.jx3.http.data.official.SkillCalculateData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import com.grafie.botjava.util.ObjectMapperUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OfficialExtensionActionsTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void shouldBuildOfficialGetQueryForEveryExtension(CaseData testCase) throws Exception {
        ApiProperties properties = properties();
        Jx3RequestUtil requestUtil = mock(Jx3RequestUtil.class);
        RequestResult requestResult = new RequestResult();
        BaseResult<Object> baseResult = new BaseResult<>();
        baseResult.setCode(200);
        baseResult.setData(responseData(testCase.regex()));
        when(requestUtil.doGetRequest(eq(testCase.regex().getMethodEnum()), anyMap()))
                .thenReturn(requestResult);
        when(requestUtil.getResultRealData(requestResult, testCase.regex().getMethodEnum()))
                .thenReturn(baseResult);
        Jx3BaseAction action = action(testCase.regex().getBaseAction(), properties, requestUtil);

        BotResponse response = action.doRequest(message(), testCase.command(), testCase.regex());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(requestUtil).doGetRequest(eq(testCase.regex().getMethodEnum()), params.capture());
        assertEquals(testCase.expectedParams(), params.getValue());
        assertEquals(org.springframework.http.HttpMethod.GET,
                testCase.regex().getMethodEnum().getHttpMethod());
        assertEquals(testCase.level(), testCase.regex().getMethodEnum().getApiLevel());
        assertNotNull(response);
        if (testCase.regex() == REGEX.ChatRecords) {
            assertEquals(BotResponse.ResponseType.IMAGE, response.getResponseType());
            assertEquals("角色聊天", response.getTemplateName());
            assertNotNull(response.getTemplateData());
        } else {
            assertEquals(BotResponse.ResponseType.TEXT, response.getResponseType());
            assertFalse(response.getContent().isBlank());
        }
    }

    @Test
    void shouldCoverAllCurrentOfficialOpenApiPathsFromInventory() throws Exception {
        String inventory = java.nio.file.Files.readString(
                java.nio.file.Path.of("docs", "JX3API_HTTP_API_INVENTORY.md"));
        java.util.Set<String> inventoryPaths = new java.util.LinkedHashSet<>();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("`(/[^`]+)`").matcher(inventory);
        while (matcher.find()) {
            inventoryPaths.add(matcher.group(1));
        }
        java.util.Set<String> methodPaths = java.util.Arrays.stream(MethodEnum.values())
                .map(MethodEnum::getMethodPath)
                .collect(java.util.stream.Collectors.toSet());

        assertEquals(78, inventoryPaths.size());
        assertTrue(methodPaths.containsAll(inventoryPaths),
                () -> "Missing official paths: " + inventoryPaths.stream()
                        .filter(path -> !methodPaths.contains(path)).toList());
    }

    private static Stream<CaseData> cases() {
        return Stream.of(
                data(REGEX.RoleAchievement, "成就查询 乾坤一掷 角色名 阴阳两界", 2,
                        Map.of("server", "乾坤一掷", "role", "角色名", "name", "阴阳两界")),
                data(REGEX.CardPreset, "名片预设 乾坤一掷 角色名", 2,
                        Map.of("server", "乾坤一掷", "name", "角色名", "ticket", "test-ticket")),
                data(REGEX.ChatRecords, "角色聊天 乾坤一掷 角色名 5 2", 2,
                        Map.of("server", "乾坤一掷", "name", "角色名", "limit", 5, "page", 2)),
                data(REGEX.EventStrategy, "奇遇攻略 阴阳两界", 1, Map.of("name", "阴阳两界")),
                data(REGEX.RankArena, "跨服名剑 乾坤一掷 1", 2,
                        Map.of("server", "乾坤一掷", "mode", 1)),
                data(REGEX.RankChampionship, "武林争霸 乾坤一掷 2", 2,
                        Map.of("server", "乾坤一掷", "camp", 2)),
                data(REGEX.RankConstable, "捕快荣誉 乾坤一掷", 2, Map.of("server", "乾坤一掷")),
                data(REGEX.RankOutlaw, "江湖浪客 乾坤一掷", 2, Map.of("server", "乾坤一掷")),
                data(REGEX.RankWanted, "决斗挑战 乾坤一掷 2", 2,
                        Map.of("server", "乾坤一掷", "mode", 2)),
                data(REGEX.SaohuaAnswer, "答案之书", 0, Map.of()),
                data(REGEX.SaohuaContext, "分类语录 疯狂星期四", 2, Map.of("name", "疯狂星期四")),
                data(REGEX.SaohuaDrink, "喝什么", 0, Map.of()),
                data(REGEX.SaohuaEat, "吃什么", 0, Map.of()),
                data(REGEX.SaohuaZhanan, "渣男语录", 0, Map.of()),
                data(REGEX.SchoolSearch, "配装搜索 万花 PVE", 2,
                        Map.of("name", "万花", "mode", "PVE", "ticket", "test-ticket")),
                data(REGEX.SkillCalculate, "急速计算 1.5", 2, Map.of("cooldown", 1.5D)),
                data(REGEX.TradeManufacture, "成本计算 乾坤一掷 成品名 1", 2,
                        Map.of("server", "乾坤一掷", "name", "成品名", "source", 1)),
                data(REGEX.TuilanAchievement, "资历分布 乾坤一掷 角色名 2 江湖历程", 2,
                        Map.of("server", "乾坤一掷", "name", "角色名", "class", 2,
                                "subclass", "江湖历程", "ticket", "test-ticket"))
        );
    }

    private static CaseData data(REGEX regex, String command, int level, Map<String, Object> expected) {
        return new CaseData(regex, command, level, expected);
    }

    private Object responseData(REGEX regex) {
        if (regex == REGEX.ChatRecords) {
            ChatRecordsData.ChatRecord record = new ChatRecordsData.ChatRecord();
            record.setZone("电信区");
            record.setServer("乾坤一掷");
            record.setRoleName("角色名");
            record.setChannel("世界");
            record.setMessage("测试聊天记录");
            record.setTime(1_767_102_878L);
            ChatRecordsData data = new ChatRecordsData();
            data.setTotal(1);
            data.setList(List.of(record));
            return data;
        }
        if (regex == REGEX.SaohuaDrink || regex == REGEX.SaohuaEat) {
            return List.of("测试推荐");
        }
        if (regex == REGEX.SkillCalculate) {
            SkillCalculateData value = new SkillCalculateData();
            value.setDuration(1.5D);
            value.setLevel(1);
            value.setNowFrame(24);
            return List.of(value);
        }
        FlexibleOfficialData data = new FlexibleOfficialData();
        if (regex == REGEX.SaohuaAnswer) {
            data.put("answer", TextNode.valueOf("顺其自然"));
            data.put("hearten", TextNode.valueOf("听从你心"));
        } else if (regex == REGEX.SaohuaContext) {
            data.put("content", TextNode.valueOf("测试语录"));
        } else if (regex == REGEX.SaohuaZhanan) {
            data.put("text", TextNode.valueOf("测试语录"));
        } else {
            data.put("name", TextNode.valueOf("测试结果"));
            ArrayNode records = ObjectMapperUtil.getObjectMapper().createArrayNode();
            data.put("data", records);
        }
        return data;
    }

    private Jx3BaseAction action(Class<? extends Jx3BaseAction> type, ApiProperties properties,
                                 Jx3RequestUtil requestUtil) throws Exception {
        Constructor<? extends Jx3BaseAction> constructor = type.getConstructor(
                ApiProperties.class, Jx3RequestUtil.class, GroupConfigurationService.class);
        return constructor.newInstance(properties, requestUtil, mock(GroupConfigurationService.class));
    }

    private ApiProperties properties() {
        ApiProperties properties = new ApiProperties();
        properties.setApiToken("level-one-token");
        properties.setApiV2Token("level-two-token");
        properties.setDefaultServer("乾坤一掷");
        properties.setTicket("test-ticket");
        return properties;
    }

    private GroupAtMessageCreateDto message() {
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setId("message-1");
        message.setGroupOpenid("group-1");
        return message;
    }

    private record CaseData(REGEX regex, String command, int level, Map<String, Object> expectedParams) {
        @Override
        public String toString() {
            return regex.name();
        }
    }
}