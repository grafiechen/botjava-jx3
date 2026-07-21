package com.grafie.botjava.jx3.http.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.data.active.monster.ActiveMonsterData;
import com.grafie.botjava.jx3.http.data.active.monster.MonsterInfo;
import com.grafie.botjava.jx3.http.data.active.monster.OtherData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActiveMonsterActionTest {

    @Test
    void shouldDeserializeOfficialRootFieldsAndLegacyDataAlias() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        ActiveMonsterData current = mapper.readValue("""
                {"week":"49","boss":"拓跋思南","list":[{"index":1,"name":"秦雷","skill":["积气法门"]}]}
                """, ActiveMonsterData.class);
        ActiveMonsterData legacy = mapper.readValue("""
                {"data":[{"name":"旧首领"}]}
                """, ActiveMonsterData.class);

        assertEquals("49", current.getWeek());
        assertEquals("拓跋思南", current.getBoss());
        assertEquals(1, current.getList().get(0).getIndex());
        assertEquals(List.of("积气法门"), current.getList().get(0).getSkill());
        assertEquals("旧首领", legacy.getList().get(0).getName());
    }

    @Test
    void shouldBuildMonsterImageWithStableNestedView() {
        OtherData extra = new OtherData();
        extra.setId(99);
        extra.setName("剑意激荡");
        extra.setList(List.of("攻击提高", "移动速度提高"));
        extra.setDescription("注意处理场地机制。");
        MonsterInfo monster = new MonsterInfo();
        monster.setIndex(1);
        monster.setName("秦雷");
        monster.setSkill(List.of("积气法门", "霞月长针"));
        monster.setData(extra);
        ActiveMonsterData apiData = new ActiveMonsterData();
        apiData.setWeek("49");
        apiData.setBoss("拓跋思南");
        apiData.setStart(1764543600L);
        apiData.setEnd(1765148400L);
        apiData.setList(List.of(monster));

        Jx3RequestUtil requestUtil = mock(Jx3RequestUtil.class);
        RequestResult requestResult = new RequestResult();
        BaseResult<Object> baseResult = new BaseResult<>();
        baseResult.setCode(200);
        baseResult.setData(apiData);
        when(requestUtil.doPostRequest(anyString(), any())).thenReturn(requestResult);
        when(requestUtil.getResultRealData(requestResult, REGEX.ActiveMonster.getMethodEnum()))
                .thenReturn(baseResult);
        ApiProperties properties = new ApiProperties();
        properties.setDefaultServer("乾坤一掷");
        ActiveMonsterAction action = new ActiveMonsterAction(
                properties, requestUtil, mock(GroupConfigurationService.class));

        BotResponse response = action.doRequest(message(), "百战 梦江南", REGEX.ActiveMonster);

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(requestUtil).doPostRequest(
                eq(REGEX.ActiveMonster.getMethodEnum().getMethodPath()), params.capture());
        assertEquals(Map.of("server", "梦江南"), params.getValue());
        assertEquals(BotResponse.ResponseType.IMAGE, response.getResponseType());
        assertEquals("百战首领", response.getTemplateName());
        Map<?, ?> templateData = (Map<?, ?>) response.getTemplateData();
        assertEquals("梦江南", templateData.get("server"));
        assertEquals("49", templateData.get("week"));
        assertEquals("拓跋思南", templateData.get("boss"));
        ActiveMonsterAction.MonsterView view =
                (ActiveMonsterAction.MonsterView) ((List<?>) templateData.get("data")).get(0);
        assertEquals(1, view.index());
        assertEquals("秦雷", view.name());
        assertEquals(List.of("积气法门", "霞月长针"), view.skills());
        assertEquals("剑意激荡", view.extraName());
        assertEquals(List.of("攻击提高", "移动速度提高"), view.effects());
    }

    private GroupAtMessageCreateDto message() {
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setId("message-1");
        message.setGroupOpenid("group-1");
        return message;
    }
}
