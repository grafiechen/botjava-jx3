package com.grafie.botjava.jx3.http.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.data.exam.ExamAnswerData;
import com.grafie.botjava.jx3.http.data.official.OfficialQueryData;
import com.grafie.botjava.jx3.http.data.server.ServerMasterData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
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

class LookupImageActionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldExposeQuestionAndAnswerOnly() {
        ExamAnswerData answer = new ExamAnswerData();
        answer.setId(42);
        answer.setQuestion("古琴有几根弦？");
        answer.setAnswer("七根");
        answer.setCorrectness(99);
        answer.setIndex(3);
        answer.setPinyin("gu qin");

        Execution execution = execute(REGEX.ExamAnswer, "科举 古琴有几根弦", List.of(answer));
        ExamAnswerAction.QuestionView view = firstView(execution.response(), ExamAnswerAction.QuestionView.class);

        assertEquals("古琴有几根弦", template(execution.response()).get("subject"));
        assertEquals("七根", view.answer());
        assertRecordExcludes(view, "id", "correctness", "index", "pinyin");
    }

    @Test
    void shouldReadCurrentServerMasterContractAndHideInternalFields() throws Exception {
        ServerMasterData data = objectMapper.readValue("""
                {"id":"master-1","center":"唯我独尊","zone":"电信区","name":"长安城",
                 "event":1,"voice":{"yy":[123]},"alias":["长安","电五长安"],
                 "slave":["乾坤一掷","斗转星移"]}
                """, ServerMasterData.class);

        Execution execution = execute(REGEX.ServerMaster, "区服 长安城", data);
        ServerMasterAction.ServerView view = assertInstanceOf(
                ServerMasterAction.ServerView.class, template(execution.response()).get("data"));

        assertEquals("长安城", execution.params().get("name"));
        assertEquals("唯我独尊", view.center());
        assertEquals(List.of("长安", "电五长安"), view.aliases());
        assertEquals(List.of("乾坤一掷", "斗转星移"), view.slaves());
        assertRecordExcludes(view, "id", "event", "voice");
    }

    @Test
    void shouldKeepLegacyServerMasterAliasesAtDtoBoundary() throws Exception {
        ServerMasterData data = objectMapper.readValue("""
                {"column":"梦江南","zone":"双线区","name":"绝代天骄",
                 "abbreviation":["绝代"],"subordinate":["剑胆琴心"]}
                """, ServerMasterData.class);

        assertEquals("梦江南", data.getCenter());
        assertEquals(List.of("绝代"), data.getAlias());
        assertEquals(List.of("剑胆琴心"), data.getSlave());
    }

    @Test
    void shouldFormatFuyouTimeWithoutGuessingStatusMeaning() {
        OfficialQueryData.ActiveNextEvent event = new OfficialQueryData.ActiveNextEvent();
        event.setZone("电信区");
        event.setServer("乾坤一掷");
        event.setStatus(1);
        event.setTime(1_764_574_651L);

        Execution execution = execute(REGEX.ActiveNextEvent, "扶摇 乾坤一掷", List.of(event));
        ActiveNextEventAction.EventView view = firstView(
                execution.response(), ActiveNextEventAction.EventView.class);

        assertEquals("乾坤一掷", template(execution.response()).get("server"));
        assertEquals("2025-12-01 15:37:31", view.time());
        assertRecordExcludes(view, "status");
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
        GroupConfigurationService configurationService = mock(GroupConfigurationService.class);
        Jx3BaseAction action = switch (regex) {
            case ExamAnswer -> new ExamAnswerAction(properties, requestUtil, configurationService);
            case ServerMaster -> new ServerMasterAction(properties, requestUtil, configurationService);
            case ActiveNextEvent -> new ActiveNextEventAction(properties, requestUtil, configurationService);
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

    private <T> T firstView(BotResponse response, Class<T> type) {
        return assertInstanceOf(type, ((List<?>) template(response).get("data")).get(0));
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
