package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.data.member.teacher.MemberTeacherData;
import com.grafie.botjava.jx3.http.data.official.OfficialQueryData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Constructor;
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

class OfficialMemberCommandActionTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("commands")
    void shouldBuildOfficialRequestParameters(CommandCase commandCase) throws Exception {
        ApiProperties properties = new ApiProperties();
        properties.setDefaultServer("梦江南");
        properties.setTicket("ticket-test");
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

        BotResponse response = action.doRequest(message(), commandCase.command(), commandCase.regex());

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
                new CommandCase("阵营拍卖", REGEX.AuctionRecords, "拍卖 乾坤一掷 玄晶",
                        Map.of("server", "乾坤一掷", "name", "玄晶", "limit", 20)),
                new CommandCase("的卢记录", REGEX.HorseRecords, "的卢 乾坤一掷",
                        Map.of("server", "乾坤一掷")),
                new CommandCase("骗子查询", REGEX.FraudDetail, "骗子 570790267",
                        Map.of("uid", 570790267L)),
                new CommandCase("未做奇遇", REGEX.LuckUnfinished, "未做奇遇 乾坤一掷 加菲",
                        Map.of("server", "乾坤一掷", "name", "加菲")),
                new CommandCase("名剑排行", REGEX.MatchAwesome, "名剑排行 33 10",
                        Map.of("mode", 33, "limit", 10, "ticket", "ticket-test")),
                new CommandCase("名剑统计", REGEX.MatchSchools, "名剑统计 55",
                        Map.of("mode", 55, "ticket", "ticket-test")),
                new CommandCase("角色信息", REGEX.RoleDetailed, "角色 乾坤一掷 加菲",
                        Map.of("server", "乾坤一掷", "name", "加菲")),
                new CommandCase("角色百战", REGEX.RoleMonster, "角色百战 乾坤一掷 加菲",
                        Map.of("server", "乾坤一掷", "name", "加菲")),
                new CommandCase("心法阵眼", REGEX.SchoolMatrix, "阵眼 花间游",
                        Map.of("name", "花间游", "ticket", "ticket-test")),
                new CommandCase("技能详情", REGEX.SchoolSkills, "技能 花间游",
                        Map.of("name", "花间游", "ticket", "ticket-test")),
                new CommandCase("技改记录", REGEX.SkillRework, "技改", Map.of()),
                new CommandCase("小药推荐", REGEX.SchoolFoods, "小药", Map.of()),
                new CommandCase("扶摇预测", REGEX.ActiveNextEvent, "扶摇 乾坤一掷",
                        Map.of("server", "乾坤一掷")),
                new CommandCase("近期奇遇", REGEX.LuckRecent, "近期奇遇 乾坤一掷",
                        Map.of("server", "乾坤一掷")),
                new CommandCase("奇遇汇总", REGEX.LuckCollect, "奇遇汇总 乾坤一掷 7",
                        Map.of("server", "乾坤一掷", "num", 7)),
                new CommandCase("师徒系统", REGEX.MemberTeacher, "师徒 1 乾坤一掷 PVE",
                        Map.of("type", 1, "server", "乾坤一掷", "keyword", "PVE")),
                new CommandCase("本服榜单", REGEX.RankStatistical, "榜单 乾坤一掷 名士五十强",
                        Map.of("server", "乾坤一掷", "name", "名士五十强")),
                new CommandCase("掉落统计", REGEX.ValuablesStatistical, "掉落 乾坤一掷 玄晶",
                        Map.of("server", "乾坤一掷", "name", "玄晶", "limit", 20)),
                new CommandCase("角色名片", REGEX.RoleShowCard, "名片 乾坤一掷 加菲",
                        Map.of("server", "乾坤一掷", "name", "加菲")),
                new CommandCase("所有名片", REGEX.RoleShowCards, "所有名片 乾坤一掷 加菲",
                        Map.of("server", "乾坤一掷", "name", "加菲"))
        );
    }

    private static GroupAtMessageCreateDto message() {
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setId("message-1");
        message.setGroupOpenid("group-1");
        return message;
    }

    private static MemberTeacherData memberTeacherData() {
        MemberTeacherData data = new MemberTeacherData();
        data.setZone("电信区");
        data.setServer("乾坤一掷");
        data.setType(1);
        data.setData(java.util.List.of());
        return data;
    }

    private static Object apiData(REGEX regex) {
        if (regex == REGEX.MemberTeacher) {
            return memberTeacherData();
        }
        if (regex == REGEX.SkillRework) {
            OfficialQueryData.SkillRework data = new OfficialQueryData.SkillRework();
            data.setTitle("武学调整");
            return java.util.List.of(data);
        }
        if (regex == REGEX.SchoolFoods) {
            OfficialQueryData.SchoolFood data = new OfficialQueryData.SchoolFood();
            data.setName("风语·灌汤包");
            return java.util.List.of(data);
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
