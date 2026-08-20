package com.grafie.botjava.jx3.http.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.data.active.ActiveCelebritiesData;
import com.grafie.botjava.jx3.http.data.active.ActiveCurrentData;
import com.grafie.botjava.jx3.http.data.active.calendar.ActiveCalendarData;
import com.grafie.botjava.jx3.http.data.active.calendar.DataInfo;
import com.grafie.botjava.jx3.http.data.active.calendar.Today;
import com.grafie.botjava.jx3.http.data.official.OfficialQueryData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import com.grafie.botjava.util.ObjectMapperUtil;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActivityAndFlowerImageActionTest {

    @Test
    void shouldBuildDailyCalendarWithoutAssumingFixedTeamSize() {
        ActiveCurrentData current = new ActiveCurrentData();
        current.setDate("2025-12-03");
        current.setWeek("三");
        current.setWar("大战！英雄冰川宫宝库");
        current.setBattle("浮香丘");
        current.setOrecar("跨服·河西瀚漠");
        current.setSchool("明教·漫漫朝圣路");
        current.setRescue("七秀·乱世");
        current.setLuck(List.of("丰丰", " ", "童心客"));
        current.setTeam(List.of("五台山·无遮大会", "会战弓月城"));

        Execution execution = execute(REGEX.ActiveCurrent, "日常 乾坤一掷 3", current);
        Map<?, ?> template = template(execution.response());
        ActiveCurrentAction.DailyView view = assertInstanceOf(
                ActiveCurrentAction.DailyView.class, template.get("data"));

        assertEquals("乾坤一掷", template.get("server"));
        assertEquals(List.of("丰丰", "童心客"), view.luck());
        assertEquals(2, view.teams().size());
        assertEquals("乾坤一掷", execution.params().get("server"));
        assertEquals(3, execution.params().get("num"));
    }

    @Test
    void shouldDeserializeAndRenderCurrentArrayPayload() throws Exception {
        ActiveCurrentData current = ObjectMapperUtil.readValue("""
                {"date":"2026-08-19","week":"三","war":"大战！英雄天龙寺",\
                 "battle":"三国古战场","orecar":"跨服·河西瀚漠","rescue":"万花·乱世",\
                 "card":["英雄九辩馆","尘归海·巨冥湾"],\
                 "draw":["苍云铁麟·成男","长歌儒风·成男","霸刀名少·成男","蓬莱仙梧·成男","凌雪冥夜·成男"],\
                 "school":"唐门·兄弟之争","leader":["白非人","宗朗"],\
                 "lucky":["崽崽","小落墨","刀豆"],\
                 "weekly":{"conn":["洛阳城·攻打应天门","洛阳·神兵迷踪"],\
                           "raid":["阆风悬城","武狱黑牢","西津渡"]}}
                """, ActiveCurrentData.class);

        Execution execution = execute(REGEX.ActiveCurrent, "日常 乾坤一掷", current);
        ActiveCurrentAction.DailyView view = assertInstanceOf(
                ActiveCurrentAction.DailyView.class, template(execution.response()).get("data"));

        assertEquals(5, view.draws().size());
        assertEquals(List.of("崽崽", "小落墨", "刀豆"), view.luck());
        assertEquals(List.of("白非人", "宗朗"), view.leaders());
        assertEquals(List.of(
                "公共任务：洛阳城·攻打应天门", "公共任务：洛阳·神兵迷踪",
                "团队秘境：阆风悬城", "团队秘境：武狱黑牢", "团队秘境：西津渡"), view.teams());
        ActiveCurrentData legacy = ObjectMapperUtil.readValue(
                "{\"draw\":\"美人画图·明教\",\"luck\":[\"丰丰\"],\"team\":[\"会战弓月城\"]}",
                ActiveCurrentData.class);
        assertEquals(List.of("美人画图·明教"), legacy.getDraw());
        assertEquals(List.of("丰丰"), legacy.getLuck());
        assertEquals(List.of("会战弓月城"), legacy.getTeam());
    }
    @Test
    void shouldBuildMonthlyCalendarWithReadableDateFieldsOnly() {
        Today today = new Today();
        today.setDate("2025-12-01");
        today.setWeek("一");
        today.setYear("2025");
        today.setMonth("12");
        today.setDay("01");
        DataInfo day = new DataInfo();
        day.setDate("2025-12-03");
        day.setWeek("三");
        day.setYear("2025");
        day.setMonth("12");
        day.setDay("03");
        day.setWar("英雄不染窟");
        day.setBattle("雪域关城");
        day.setLuck(List.of("丰丰", ""));
        day.setCard(List.of("英雄迷渊岛"));
        ActiveCalendarData calendar = new ActiveCalendarData();
        calendar.setToday(today);
        calendar.setData(List.of(day));

        Execution execution = execute(REGEX.ActiveListCalendar, "月历 15", calendar);
        Map<?, ?> template = template(execution.response());
        ActiveListCalendarAction.CalendarDayView view = assertInstanceOf(
                ActiveListCalendarAction.CalendarDayView.class, ((List<?>) template.get("data")).get(0));

        assertEquals("2025-12-03", view.date());
        assertEquals(List.of("丰丰"), view.luck());
        assertRecordExcludes(view, "year", "month", "day");
        assertEquals(15, execution.params().get("num"));
    }

    @Test
    void shouldMapCurrentCelebritiesAliasesAndExcludeIconCode() throws Exception {
        ActiveCelebritiesData event = new ObjectMapper().readValue("""
                {"map":"晟江","stage":"恶霸出浴","site":"白菰里",\
                 "desc":"公共任务：击退恶霸黄七。","icon":"7","time":"12:56"}
                """, ActiveCelebritiesData.class);

        Execution execution = execute(REGEX.ActiveCelebrities, "行侠 楚天社", List.of(event));
        ActiveCelebritiesAction.EventView view = assertInstanceOf(
                ActiveCelebritiesAction.EventView.class,
                ((List<?>) template(execution.response()).get("data")).get(0));

        assertEquals("晟江", view.mapName());
        assertEquals("恶霸出浴", view.event());
        assertEquals("楚天社", execution.params().get("name"));
        assertRecordExcludes(view, "icon");
    }

    @Test
    void shouldFlattenDynamicFlowerServerKeys() {
        OfficialQueryData.Flower flower = new OfficialQueryData.Flower();
        flower.setName("一级绣球花");
        flower.setColor("红，白，紫");
        flower.setPrice(1.5D);
        flower.setLine(List.of("6", "25", "18"));
        OfficialQueryData.HomeFlower flowers = new OfficialQueryData.HomeFlower();
        flowers.putServer("九寨沟·镜海", List.of(flower));

        Execution execution = execute(REGEX.HomeFlower, "鲜花 乾坤一掷 绣球花", flowers);
        Map<?, ?> template = template(execution.response());
        HomeFlowerAction.ServerFlowerView group = assertInstanceOf(
                HomeFlowerAction.ServerFlowerView.class, ((List<?>) template.get("data")).get(0));

        assertEquals("乾坤一掷", template.get("server"));
        assertEquals("绣球花", template.get("name"));
        assertEquals("九寨沟·镜海", group.server());
        assertEquals(List.of("6", "25", "18"), group.flowers().get(0).lines());
    }

    private Execution execute(REGEX regex, String command, Object apiData) {
        Jx3RequestUtil requestUtil = mock(Jx3RequestUtil.class);
        RequestResult requestResult = new RequestResult();
        BaseResult<Object> baseResult = new BaseResult<>();
        baseResult.setCode(200);
        baseResult.setData(apiData);
        when(requestUtil.doPostRequest(anyString(), any())).thenReturn(requestResult);
        when(requestUtil.getResultRealData(requestResult, regex.getMethodEnum())).thenReturn(baseResult);
        ApiProperties properties = new ApiProperties();
        properties.setDefaultServer("梦江南");
        Jx3BaseAction action = switch (regex) {
            case ActiveCurrent -> new ActiveCurrentAction(properties, requestUtil, mock(GroupConfigurationService.class));
            case ActiveListCalendar -> new ActiveListCalendarAction(properties, requestUtil, mock(GroupConfigurationService.class));
            case ActiveCelebrities -> new ActiveCelebritiesAction(properties, requestUtil, mock(GroupConfigurationService.class));
            case HomeFlower -> new HomeFlowerAction(properties, requestUtil, mock(GroupConfigurationService.class));
            default -> throw new IllegalArgumentException("不支持的测试指令：" + regex);
        };

        BotResponse response = action.doRequest(message(), command, regex);
        assertEquals(BotResponse.ResponseType.IMAGE, response.getResponseType());
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(requestUtil).doPostRequest(anyString(), captor.capture());
        return new Execution(response, captor.getValue());
    }

    private Map<?, ?> template(BotResponse response) {
        return assertInstanceOf(Map.class, response.getTemplateData());
    }

    private void assertRecordExcludes(Object record, String... names) {
        List<String> excluded = List.of(names);
        assertFalse(Arrays.stream(record.getClass().getRecordComponents())
                .anyMatch(component -> excluded.contains(component.getName())));
    }

    private GroupAtMessageCreateDto message() {
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setId("message-1");
        message.setGroupOpenid("group-1");
        return message;
    }

    private record Execution(BotResponse response, Map<String, Object> params) {
    }
}
