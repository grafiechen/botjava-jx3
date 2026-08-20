package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OfficialIndependentTextActionTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("commands")
    void shouldLetConcreteActionOwnParametersAndDeclaredResponse(CommandCase commandCase) {
        ApiProperties properties = new ApiProperties();
        properties.setDefaultServer("梦江南");
        Jx3RequestUtil requestUtil = mock(Jx3RequestUtil.class);
        RequestResult requestResult = new RequestResult();
        BaseResult<Object> baseResult = new BaseResult<>();
        baseResult.setCode(200);
        baseResult.setData(Map.of("title", "测试结果"));
        when(requestUtil.doPostRequest(anyString(), any())).thenReturn(requestResult);
        when(requestUtil.getResultRealData(requestResult, commandCase.regex().getMethodEnum()))
                .thenReturn(baseResult);
        Jx3BaseAction action = action(commandCase.regex(), properties, requestUtil);

        BotResponse response = action.doRequest(message(), commandCase.command(), commandCase.regex());

        assertEquals(action.getClass(), commandCase.regex().getBaseAction());
        assertNotNull(response);
        assertEquals(commandCase.responseType(), response.getResponseType());
        if (commandCase.responseType() == BotResponse.ResponseType.IMAGE) {
            assertNotNull(response.getTemplateName());
            assertNotNull(response.getTemplateData());
        } else {
            assertNotNull(response.getContent());
        }
        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(requestUtil).doPostRequest(
                eq(commandCase.regex().getMethodEnum().getMethodPath()), params.capture());
        assertEquals(commandCase.expectedParams(), params.getValue());
    }

    private static Stream<CommandCase> commands() {
        return Stream.of(
                imageCommand("随机名片", REGEX.RoleShowRandom, "随机名片 乾坤一掷 萝莉 万花",
                        Map.of("server", "乾坤一掷", "body", "萝莉", "force", "万花")),
                imageCommand("缓存名片", REGEX.RoleShowCached, "缓存名片 乾坤一掷 加菲",
                        Map.of("server", "乾坤一掷", "name", "加菲")),
                imageCommand("贴吧物价", REGEX.TiebaItemRecords, "贴吧物价 乾坤一掷 狐金",
                        Map.of("server", "乾坤一掷", "name", "狐金", "limit", 10)),
                command("搜索物品", REGEX.TradeItemSearch, "搜索物品 十五",
                        Map.of("name", "十五")),
                imageCommand("副本解密", REGEX.MechCalculator, "副本解密", Map.of()),
                imageCommand("统战歪歪", REGEX.DuowanStatistics, "统战 乾坤一掷",
                        Map.of("server", "乾坤一掷")),
                command("八卦帖子", REGEX.TiebaRandom, "八卦 818 乾坤一掷",
                        Map.of("tags", "818", "server", "乾坤一掷", "limit", 1)),
                command("舔狗日记", REGEX.SaohuaContent, "舔狗日记", Map.of())
        );
    }

    private static Jx3BaseAction action(REGEX regex, ApiProperties properties,
                                        Jx3RequestUtil requestUtil) {
        GroupConfigurationService configurationService = mock(GroupConfigurationService.class);
        return switch (regex) {
            case RoleShowRandom -> new RoleShowRandomAction(properties, requestUtil, configurationService);
            case RoleShowCached -> new RoleShowCachedAction(properties, requestUtil, configurationService);
            case TiebaItemRecords -> new TiebaItemRecordsAction(properties, requestUtil, configurationService);
            case TradeItemSearch -> new TradeItemSearchAction(properties, requestUtil, configurationService);
            case MechCalculator -> new MechCalculatorAction(properties, requestUtil, configurationService);
            case DuowanStatistics -> new DuowanStatisticsAction(properties, requestUtil, configurationService);
            case TiebaRandom -> new TiebaRandomAction(properties, requestUtil, configurationService);
            case SaohuaContent -> new SaohuaContentAction(properties, requestUtil, configurationService);
            default -> throw new IllegalArgumentException("未支持的测试指令：" + regex);
        };
    }

    private static CommandCase command(String name, REGEX regex, String command,
                                       Map<String, Object> params) {
        return new CommandCase(name, regex, command, params, BotResponse.ResponseType.TEXT);
    }

    private static CommandCase imageCommand(String name, REGEX regex, String command,
                                            Map<String, Object> params) {
        return new CommandCase(name, regex, command, params, BotResponse.ResponseType.IMAGE);
    }

    private static GroupAtMessageCreateDto message() {
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setId("message-1");
        message.setGroupOpenid("group-1");
        return message;
    }

    private record CommandCase(String name, REGEX regex, String command,
                               Map<String, Object> expectedParams, BotResponse.ResponseType responseType) {
        @Override
        public String toString() {
            return name;
        }
    }
}
