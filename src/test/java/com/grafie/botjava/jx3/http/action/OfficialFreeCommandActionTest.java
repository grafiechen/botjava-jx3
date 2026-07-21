package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.data.home.HomeFurnitureData;
import com.grafie.botjava.jx3.http.data.home.HomeTravelData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Constructor;
import java.util.List;
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

class OfficialFreeCommandActionTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("commands")
    void shouldBuildOfficialRequestParameters(CommandCase commandCase) throws Exception {
        ApiProperties properties = new ApiProperties();
        properties.setDefaultServer("梦江南");
        Jx3RequestUtil requestUtil = mock(Jx3RequestUtil.class);
        GroupConfigurationService groupConfigurationService = mock(GroupConfigurationService.class);
        RequestResult requestResult = new RequestResult();
        BaseResult<Object> baseResult = new BaseResult<>();
        baseResult.setCode(200);
        baseResult.setData(apiData(commandCase.regex()));
        when(requestUtil.doPostRequest(anyString(), any())).thenReturn(requestResult);
        when(requestUtil.getResultRealData(requestResult, commandCase.regex().getMethodEnum()))
                .thenReturn(baseResult);
        Jx3BaseAction action = instantiate(
                commandCase.regex().getBaseAction(), properties, requestUtil, groupConfigurationService);

        BotResponse response = action.doRequest(
                message(), commandCase.command(), commandCase.regex());

        assertNotNull(response);
        if (response.getResponseType() == BotResponse.ResponseType.IMAGE) {
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
                new CommandCase("活动月历", REGEX.ActiveListCalendar, "月历 15", Map.of("num", 15)),
                new CommandCase("行侠事件", REGEX.ActiveCelebrities, "行侠 楚天社", Map.of("name", "楚天社")),
                new CommandCase("科举答题", REGEX.ExamAnswer, "科举 古琴有几根弦",
                        Map.of("subject", "古琴有几根弦", "limit", 5)),
                new CommandCase("家园鲜花", REGEX.HomeFlower, "鲜花 乾坤一掷 绣球花",
                        Map.of("server", "乾坤一掷", "name", "绣球花")),
                new CommandCase("家园装饰", REGEX.HomeFurniture, "家具 龙门香梦", Map.of("name", "龙门香梦")),
                new CommandCase("器物图谱", REGEX.HomeTravel, "器物 万花", Map.of("name", "万花")),
                new CommandCase("新闻资讯", REGEX.NewsAllNews, "新闻 3", Map.of("limit", 3)),
                new CommandCase("搜索区服", REGEX.ServerMaster, "区服 双梦镇", Map.of("name", "双梦镇"))
        );
    }

    private static GroupAtMessageCreateDto message() {
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setId("message-1");
        message.setGroupOpenid("group-1");
        return message;
    }

    private static Object apiData(REGEX regex) {
        if (regex == REGEX.HomeFurniture) {
            HomeFurnitureData data = new HomeFurnitureData();
            data.setName("龙门香梦");
            return List.of(data);
        }
        if (regex == REGEX.HomeTravel) {
            HomeTravelData data = new HomeTravelData();
            data.setName("日晷");
            return List.of(data);
        }
        return Map.of("title", "测试结果");
    }

    private static Jx3BaseAction instantiate(Class<? extends Jx3BaseAction> type, ApiProperties properties,
                                             Jx3RequestUtil requestUtil, GroupConfigurationService groupConfigurationService)
            throws Exception {
        Constructor<? extends Jx3BaseAction> constructor = type.getConstructor(
                ApiProperties.class, Jx3RequestUtil.class, GroupConfigurationService.class);
        return constructor.newInstance(properties, requestUtil, groupConfigurationService);
    }

    private record CommandCase(String name, REGEX regex, String command, Map<String, Object> expectedParams) {
        @Override
        public String toString() {
            return name;
        }
    }
}
