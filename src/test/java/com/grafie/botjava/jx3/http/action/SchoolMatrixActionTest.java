package com.grafie.botjava.jx3.http.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.data.school.matirx.DescriptiveSkill;
import com.grafie.botjava.jx3.http.data.school.matirx.SchoolMatrixData;
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

class SchoolMatrixActionTest {

    @Test
    void shouldDeserializeOfficialDataAndLegacyDescsAlias() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        SchoolMatrixData current = mapper.readValue("""
                {"name":"花间游","skillName":"七绝逍遥阵",
                 "data":[{"desc":"阅历提高5%，声望提高5%，内功基础攻击力提高5%。","level":1,"name":"一重粗识"}]}
                """, SchoolMatrixData.class);
        SchoolMatrixData legacy = mapper.readValue("""
                {"name":"旧心法","skillName":"旧阵眼","descs":[{"desc":"旧效果","level":2,"name":"二重略懂"}]}
                """, SchoolMatrixData.class);

        assertEquals("花间游", current.getName());
        assertEquals("七绝逍遥阵", current.getSkillName());
        assertEquals("一重粗识", current.getEffects().get(0).getName());
        assertEquals("二重略懂", legacy.getEffects().get(0).getName());
    }

    @Test
    void shouldBuildMatrixImageWithStableEffects() {
        DescriptiveSkill effect = new DescriptiveSkill();
        effect.setLevel(1);
        effect.setName("一重粗识");
        effect.setDescription("阅历提高5%，声望提高5%，内功基础攻击力提高5%。");
        SchoolMatrixData apiData = new SchoolMatrixData();
        apiData.setName("花间游");
        apiData.setSkillName("七绝逍遥阵");
        apiData.setEffects(List.of(effect));

        Jx3RequestUtil requestUtil = mock(Jx3RequestUtil.class);
        RequestResult requestResult = new RequestResult();
        BaseResult<Object> baseResult = new BaseResult<>();
        baseResult.setCode(200);
        baseResult.setData(apiData);
        when(requestUtil.doPostRequest(anyString(), any())).thenReturn(requestResult);
        when(requestUtil.getResultRealData(requestResult, REGEX.SchoolMatrix.getMethodEnum()))
                .thenReturn(baseResult);
        ApiProperties properties = new ApiProperties();
        properties.setTicket("ticket-test");
        SchoolMatrixAction action = new SchoolMatrixAction(
                properties, requestUtil, mock(GroupConfigurationService.class));

        BotResponse response = action.doRequest(message(), "阵眼 花间游", REGEX.SchoolMatrix);

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(requestUtil).doPostRequest(
                eq(REGEX.SchoolMatrix.getMethodEnum().getMethodPath()), params.capture());
        assertEquals(Map.of("name", "花间游", "ticket", "ticket-test"), params.getValue());
        assertEquals(BotResponse.ResponseType.IMAGE, response.getResponseType());
        assertEquals("心法阵眼", response.getTemplateName());
        Map<?, ?> templateData = (Map<?, ?>) response.getTemplateData();
        assertEquals("花间游", templateData.get("name"));
        assertEquals("七绝逍遥阵", templateData.get("skillName"));
        SchoolMatrixAction.EffectView view =
                (SchoolMatrixAction.EffectView) ((List<?>) templateData.get("effects")).get(0);
        assertEquals(1, view.level());
        assertEquals("一重粗识", view.name());
    }

    private GroupAtMessageCreateDto message() {
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setId("message-1");
        message.setGroupOpenid("group-1");
        return message;
    }
}
