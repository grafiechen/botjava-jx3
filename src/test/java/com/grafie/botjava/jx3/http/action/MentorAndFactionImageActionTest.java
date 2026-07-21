package com.grafie.botjava.jx3.http.action;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.data.member.teacher.MemberTeacherData;
import com.grafie.botjava.jx3.http.data.server.ServerAntiviceData;
import com.grafie.botjava.jx3.http.data.server.ServerEventData;
import com.grafie.botjava.jx3.http.data.server.sand.ServerSandData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MentorAndFactionImageActionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldDeserializeOfficialMentorOuterFields() throws Exception {
        MemberTeacherData result = objectMapper.readValue("""
                {"zone":"电信区","server":"长安城","type":2,"time":1764582410,
                 "data":[{"roleId":26276059,"roleName":"不见风澜","roleLevel":130,
                 "campName":"恶人谷","tongName":"夜寐","tongMasterName":"有只鱼",
                 "bodyId":1,"bodyName":"成男","forceId":213,"forceName":"刀宗",
                 "comment":"找个一起打名剑大会的师父"}]}
                """, MemberTeacherData.class);

        assertEquals(2, result.getType());
        assertEquals("2025-12-01 17:46:50", result.getTime());
        assertEquals("不见风澜", result.getData().get(0).getRoleName());
    }

    @Test
    void shouldDeserializeCurrentFactionEventFields() throws Exception {
        JavaType type = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, ServerEventData.class);
        List<ServerEventData> result = objectMapper.readValue("""
                [{"id":11005,"camp_name":"恶人谷","fenxian_name":"梦江南",
                  "friend_name":"唯我独尊","role_name":"欧薏米·天鹅坪",
                  "seize_time":1776240427}]
                """, type);

        assertEquals("梦江南", result.get(0).getFenxianName());
        assertEquals("唯我独尊", result.get(0).getFriendName());
        assertNotNull(result.get(0).getSeizeTime());
    }

    @Test
    void shouldBuildMentorAndSandViewsWithoutInternalIds() throws Exception {
        MemberTeacherData mentor = objectMapper.readValue("""
                {"zone":"电信区","server":"长安城","type":2,"time":1764582410,
                 "data":[{"roleId":26276059,"roleName":"不见风澜","roleLevel":130,
                 "campName":"恶人谷","tongName":"夜寐","tongMasterName":"有只鱼",
                 "bodyId":1,"bodyName":"成男","forceId":213,"forceName":"刀宗",
                 "comment":"找个一起打名剑大会的师父"}]}
                """, MemberTeacherData.class);
        ServerSandData sand = objectMapper.readValue("""
                {"zone":"电信区","server":"长安城","reset":0,"update":1764652005,
                 "data":[{"tongId":71205,"tongName":"追梦烟雨","castleId":131,
                 "castleName":"金门关","masterId":7312844,"masterName":"追梦的道士",
                 "campId":1,"campName":"浩气盟"}]}
                """, ServerSandData.class);

        BotResponse mentorResponse = execute(REGEX.MemberTeacher, "师徒 2 长安城 PVP", mentor);
        BotResponse sandResponse = execute(REGEX.ServerSand, "沙盘 长安城", sand);

        assertEquals("师徒系统", mentorResponse.getTemplateName());
        Map<?, ?> mentorTemplate = assertInstanceOf(Map.class, mentorResponse.getTemplateData());
        MemberTeacherAction.MentorView mentorView = assertInstanceOf(
                MemberTeacherAction.MentorView.class, ((List<?>) mentorTemplate.get("data")).get(0));
        assertEquals("不见风澜", mentorView.roleName());
        assertRecordExcludes(mentorView, "roleId", "bodyId", "forceId", "type");

        assertEquals("阵营沙盘", sandResponse.getTemplateName());
        Map<?, ?> sandTemplate = assertInstanceOf(Map.class, sandResponse.getTemplateData());
        ServerSandAction.CastleView castleView = assertInstanceOf(
                ServerSandAction.CastleView.class, ((List<?>) sandTemplate.get("data")).get(0));
        assertEquals("金门关", castleView.castleName());
        assertRecordExcludes(castleView, "tongId", "castleId", "masterId", "campId", "reset");
    }

    @Test
    void shouldBuildFactionEventViewsWithoutRecordIds() throws Exception {
        ServerEventData faction = objectMapper.readValue("""
                {"id":11005,"camp_name":"恶人谷","fenxian_name":"梦江南",
                 "friend_name":"唯我独尊","role_name":"欧薏米·天鹅坪",
                 "seize_time":1776240427}
                """, ServerEventData.class);
        ServerAntiviceData smite = objectMapper.readValue("""
                {"id":19897,"zone":"电信区","server":"唯我独尊",
                 "map_name":"银霜口","time":1776227999}
                """, ServerAntiviceData.class);

        BotResponse factionResponse = execute(REGEX.ServerEvent, "阵营事件", List.of(faction));
        BotResponse smiteResponse = execute(REGEX.ServerAntivice, "诛恶事件", List.of(smite));

        ServerEventAction.EventView factionView = firstView(factionResponse, ServerEventAction.EventView.class);
        assertEquals("梦江南", factionView.fenxianName());
        assertRecordExcludes(factionView, "id");
        ServerAntiviceAction.SmiteView smiteView = firstView(smiteResponse, ServerAntiviceAction.SmiteView.class);
        assertEquals("银霜口", smiteView.mapName());
        assertRecordExcludes(smiteView, "id");
    }

    private BotResponse execute(REGEX regex, String command, Object apiData) {
        ApiProperties properties = new ApiProperties();
        properties.setDefaultServer("梦江南");
        Jx3RequestUtil requestUtil = mock(Jx3RequestUtil.class);
        RequestResult requestResult = new RequestResult();
        BaseResult<Object> baseResult = new BaseResult<>();
        baseResult.setCode(200);
        baseResult.setData(apiData);
        when(requestUtil.doPostRequest(anyString(), any())).thenReturn(requestResult);
        when(requestUtil.getResultRealData(requestResult, regex.getMethodEnum())).thenReturn(baseResult);
        Jx3BaseAction action = switch (regex) {
            case MemberTeacher -> new MemberTeacherAction(properties, requestUtil, mock(GroupConfigurationService.class));
            case ServerSand -> new ServerSandAction(properties, requestUtil, mock(GroupConfigurationService.class));
            case ServerEvent -> new ServerEventAction(properties, requestUtil, mock(GroupConfigurationService.class));
            case ServerAntivice -> new ServerAntiviceAction(properties, requestUtil, mock(GroupConfigurationService.class));
            default -> throw new IllegalArgumentException("不支持的测试指令：" + regex);
        };

        BotResponse response = action.doRequest(message(), command, regex);
        assertEquals(BotResponse.ResponseType.IMAGE, response.getResponseType());
        return response;
    }

    private <T> T firstView(BotResponse response, Class<T> viewType) {
        Map<?, ?> template = assertInstanceOf(Map.class, response.getTemplateData());
        return assertInstanceOf(viewType, ((List<?>) template.get("data")).get(0));
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
}
