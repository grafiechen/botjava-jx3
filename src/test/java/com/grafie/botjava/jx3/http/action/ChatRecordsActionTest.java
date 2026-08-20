package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.MethodEnum;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.data.official.ChatRecordsData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import com.grafie.botjava.util.ObjectMapperUtil;
import com.grafie.botjava.util.TimeUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatRecordsActionTest {

    @Test
    void shouldDeserializeRealResponseAndBuildReadableImageList() throws Exception {
        ChatRecordsData data = ObjectMapperUtil.readValue("""
                {
                  "total": 36,
                  "list": [{
                    "zone": "电信区",
                    "server": "乾坤一掷",
                    "roleName": "琉枫",
                    "roleId": "5715151",
                    "globalId": "306244774666908879",
                    "channel": "世界",
                    "message": "[跨服房间招募·25人普通会战弓月城]【千机】大小M 提升速 来T和奶",
                    "time": 1767102878,
                    "futureField": "ignored"
                  }],
                  "futureRootField": true
                }
                """, ChatRecordsData.class);
        assertEquals(36, data.getTotal());
        assertEquals("琉枫", data.getList().getFirst().getRoleName());
        assertEquals("306244774666908879", data.getList().getFirst().getGlobalId());

        ApiProperties properties = new ApiProperties();
        properties.setApiV2Token("level-two-token");
        Jx3RequestUtil requestUtil = mock(Jx3RequestUtil.class);
        RequestResult requestResult = new RequestResult();
        BaseResult<Object> baseResult = new BaseResult<>();
        baseResult.setCode(200);
        baseResult.setData(data);
        when(requestUtil.doGetRequest(eq(MethodEnum.DATA_CHAT_RECORDS), anyMap()))
                .thenReturn(requestResult);
        when(requestUtil.getResultRealData(requestResult, MethodEnum.DATA_CHAT_RECORDS))
                .thenReturn(baseResult);
        ChatRecordsAction action = new ChatRecordsAction(
                properties, requestUtil, mock(GroupConfigurationService.class));

        BotResponse response = action.doRequest(message(),
                "角色聊天 乾坤一掷 琉枫 20 1", REGEX.ChatRecords);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(requestUtil).doGetRequest(eq(MethodEnum.DATA_CHAT_RECORDS), params.capture());
        assertEquals(Map.of("server", "乾坤一掷", "name", "琉枫", "limit", 20, "page", 1),
                params.getValue());
        assertEquals(BotResponse.ResponseType.IMAGE, response.getResponseType());
        assertEquals("角色聊天", response.getTemplateName());
        Map<?, ?> template = assertInstanceOf(Map.class, response.getTemplateData());
        assertEquals("乾坤一掷", template.get("server"));
        assertEquals("琉枫", template.get("roleName"));
        assertEquals(36, template.get("total"));
        assertEquals(1, template.get("page"));
        List<?> records = assertInstanceOf(List.class, template.get("data"));
        ChatRecordsAction.ChatRecordView view = assertInstanceOf(
                ChatRecordsAction.ChatRecordView.class, records.getFirst());
        assertEquals("世界", view.channel());
        assertEquals("[跨服房间招募·25人普通会战弓月城]【千机】大小M 提升速 来T和奶", view.message());
        assertEquals(TimeUtils.timeFormatting(1_767_102_878L), view.time());
        assertFalse(Arrays.stream(view.getClass().getRecordComponents())
                .anyMatch(component -> component.getName().equals("roleId")
                        || component.getName().equals("globalId")));
    }

    private GroupAtMessageCreateDto message() {
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setId("message-1");
        message.setGroupOpenid("group-1");
        return message;
    }
}
