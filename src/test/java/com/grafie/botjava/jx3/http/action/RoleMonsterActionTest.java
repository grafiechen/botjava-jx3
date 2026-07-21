package com.grafie.botjava.jx3.http.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.data.role.monster.MonsterSkill;
import com.grafie.botjava.jx3.http.data.role.monster.RoleMonsterData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoleMonsterActionTest {

    @Test
    void shouldDeserializeOfficialAndLegacyFields() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        RoleMonsterData current = mapper.readValue("""
                {
                  "zone":"电信区","server":"唯我独尊","role_name":"夜温言@长安城",
                  "role_id":"26468709","global_id":"288230376159033247",
                  "skill_stamina":236400,"skill_energy":237600,"skill_count":131,
                  "skill_list":[{"skill_cost":1,"skill_name":"空穴来风","leader_name":"冯度",
                    "skill_color":6,"skill_level":10,"is_deprecated":false}],
                  "update_time":1763064756
                }
                """, RoleMonsterData.class);
        RoleMonsterData legacy = mapper.readValue("""
                {"zoneName":"旧大区","serverName":"旧服","roleName":"旧角色",
                 "gameEnergy":"100","gameStamina":"200","skillCount":"3",
                 "skillList":[{"nCost":2,"nColor":4,"nLevel":6,
                   "szBossName":"旧首领","szSkillName":"旧技能","bDeprecated":true}],
                 "updateTime":"1763064756"}
                """, RoleMonsterData.class);

        assertEquals("电信区", current.getZone());
        assertEquals(237600L, current.getSkillEnergy());
        assertEquals(236400L, current.getSkillStamina());
        assertEquals(131, current.getSkillCount());
        assertEquals("2025-11-14 04:12:36", current.getUpdateTime());
        assertEquals("空穴来风", current.getSkillList().get(0).getSkillName());
        assertFalse(current.getSkillList().get(0).getDeprecated());
        assertEquals("旧服", legacy.getServer());
        assertEquals(100L, legacy.getSkillEnergy());
        assertEquals("旧技能", legacy.getSkillList().get(0).getSkillName());
        assertEquals(true, legacy.getSkillList().get(0).getDeprecated());
    }

    @Test
    void shouldBuildRoleMonsterImageWithStableSkillView() {
        MonsterSkill skill = new MonsterSkill();
        skill.setInputSkillId(1001L);
        skill.setOutputSkillId(1002L);
        skill.setSkillName("空穴来风");
        skill.setLeaderName("冯度");
        skill.setCost(1);
        skill.setColor(6);
        skill.setLevel(10);
        skill.setDeprecated(false);
        RoleMonsterData apiData = new RoleMonsterData();
        apiData.setZone("电信区");
        apiData.setServer("唯我独尊");
        apiData.setRoleName("夜温言@长安城");
        apiData.setRoleId("26468709");
        apiData.setGlobalRoleId("288230376159033247");
        apiData.setSkillEnergy(237600L);
        apiData.setSkillStamina(236400L);
        apiData.setSkillCount(131);
        apiData.setUpdateTime(1763064756L);
        apiData.setSkillList(List.of(skill));

        Jx3RequestUtil requestUtil = mock(Jx3RequestUtil.class);
        RequestResult requestResult = new RequestResult();
        BaseResult<Object> baseResult = new BaseResult<>();
        baseResult.setCode(200);
        baseResult.setData(apiData);
        when(requestUtil.doPostRequest(anyString(), any())).thenReturn(requestResult);
        when(requestUtil.getResultRealData(requestResult, REGEX.RoleMonster.getMethodEnum()))
                .thenReturn(baseResult);
        ApiProperties properties = new ApiProperties();
        properties.setDefaultServer("乾坤一掷");
        RoleMonsterAction action = new RoleMonsterAction(
                properties, requestUtil, mock(GroupConfigurationService.class));

        BotResponse response = action.doRequest(message(), "角色百战 唯我独尊 夜温言", REGEX.RoleMonster);

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(requestUtil).doPostRequest(
                eq(REGEX.RoleMonster.getMethodEnum().getMethodPath()), params.capture());
        assertEquals(Map.of("server", "唯我独尊", "name", "夜温言"), params.getValue());
        assertEquals(BotResponse.ResponseType.IMAGE, response.getResponseType());
        assertEquals("角色百战", response.getTemplateName());
        Map<?, ?> templateData = (Map<?, ?>) response.getTemplateData();
        assertEquals("电信区", templateData.get("zone"));
        assertEquals("唯我独尊", templateData.get("server"));
        assertEquals("夜温言@长安城", templateData.get("roleName"));
        assertEquals(237600L, templateData.get("skillEnergy"));
        RoleMonsterAction.SkillView view =
                (RoleMonsterAction.SkillView) ((List<?>) templateData.get("skills")).get(0);
        assertEquals("空穴来风", view.name());
        assertEquals("冯度", view.leaderName());
        assertEquals(10, view.level());
        assertFalse(view.deprecated());
    }

    private GroupAtMessageCreateDto message() {
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setId("message-1");
        message.setGroupOpenid("group-1");
        return message;
    }
}
