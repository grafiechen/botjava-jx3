package com.grafie.botjava.jx3.http.action;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.data.school.skill.SchoolSkillsData;
import com.grafie.botjava.jx3.http.data.school.skill.Skill;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SchoolSkillsActionTest {

    @Test
    void shouldDeserializeOfficialGroupedSkills() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, SchoolSkillsData.class);

        List<SchoolSkillsData> groups = mapper.readValue("""
                [{"class":"太素九针","data":[{"name":"锋针","simpleDesc":"辅助技，非战斗状态下救治重伤的友方目标。",
                  "desc":"救治重伤的友方目标。","specialDesc":"第其身而锋其末。","interval":"无调息时间",
                  "consumption":"","distance":"20尺","icon":"https://example.invalid/skill.png",
                  "kind":"技能","subKind":"万花","releaseType":"释放10秒","weapon":"笔类"}]}]
                """, type);

        assertEquals("太素九针", groups.get(0).getCategory());
        assertEquals("锋针", groups.get(0).getSkills().get(0).getName());
        assertEquals("20尺", groups.get(0).getSkills().get(0).getDistance());
    }

    @Test
    void shouldBuildSkillsImageWithoutExternalIcon() {
        Skill skill = new Skill();
        skill.setName("锋针");
        skill.setSimpleDescription("辅助技，非战斗状态下救治重伤的友方目标。");
        skill.setDescription("救治重伤的友方目标。");
        skill.setSpecialDescription("第其身而锋其末。");
        skill.setInterval("无调息时间");
        skill.setDistance("20尺");
        skill.setIcon("https://example.invalid/skill.png");
        skill.setKind("技能");
        skill.setSubKind("万花");
        skill.setReleaseType("释放10秒");
        skill.setWeapon("笔类");
        SchoolSkillsData group = new SchoolSkillsData();
        group.setCategory("太素九针");
        group.setSkills(List.of(skill));

        Jx3RequestUtil requestUtil = mock(Jx3RequestUtil.class);
        RequestResult requestResult = new RequestResult();
        BaseResult<Object> baseResult = new BaseResult<>();
        baseResult.setCode(200);
        baseResult.setData(List.of(group));
        when(requestUtil.doPostRequest(anyString(), any())).thenReturn(requestResult);
        when(requestUtil.getResultRealData(requestResult, REGEX.SchoolSkills.getMethodEnum()))
                .thenReturn(baseResult);
        ApiProperties properties = new ApiProperties();
        properties.setTicket("ticket-test");
        SchoolSkillsAction action = new SchoolSkillsAction(
                properties, requestUtil, mock(GroupConfigurationService.class));

        BotResponse response = action.doRequest(message(), "技能 花间游", REGEX.SchoolSkills);

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(requestUtil).doPostRequest(
                eq(REGEX.SchoolSkills.getMethodEnum().getMethodPath()), params.capture());
        assertEquals(Map.of("name", "花间游", "ticket", "ticket-test"), params.getValue());
        assertEquals(BotResponse.ResponseType.IMAGE, response.getResponseType());
        assertEquals("技能详情", response.getTemplateName());
        Map<?, ?> templateData = (Map<?, ?>) response.getTemplateData();
        assertEquals("花间游", templateData.get("name"));
        SchoolSkillsAction.SkillGroupView groupView =
                (SchoolSkillsAction.SkillGroupView) ((List<?>) templateData.get("groups")).get(0);
        SchoolSkillsAction.SkillView view = groupView.skills().get(0);
        assertEquals("太素九针", groupView.category());
        assertEquals("锋针", view.name());
        assertEquals("20尺", view.distance());
        assertFalse(Arrays.stream(view.getClass().getRecordComponents())
                .anyMatch(component -> component.getName().equals("icon")));
    }

    private GroupAtMessageCreateDto message() {
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setId("message-1");
        message.setGroupOpenid("group-1");
        return message;
    }
}
