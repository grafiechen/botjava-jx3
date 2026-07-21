package com.grafie.botjava.jx3.http.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.data.official.OfficialQueryData;
import com.grafie.botjava.jx3.http.data.tieba.TiebaItemRecordsData;
import com.grafie.botjava.jx3.http.data.trade.TradeDemonData;
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

class MarketAndUtilityImageActionTest {

    @Test
    void shouldBuildReadableTiebaRecordWithoutPostIdentifiers() {
        TiebaItemRecordsData record = new TiebaItemRecordsData();
        record.setId(12);
        record.setZone("电信区");
        record.setServer("乾坤一掷");
        record.setName("狐金");
        record.setUrl(998877L);
        record.setContext("近期成交参考");
        record.setReply(18L);
        record.setToken("internal-token");
        record.setFloor(6);
        record.setTime(1_733_270_400L);

        Execution execution = execute(REGEX.TiebaItemRecords, "贴吧物价 乾坤一掷 狐金", List.of(record));
        TiebaItemRecordsAction.RecordView view = firstView(execution.response(), TiebaItemRecordsAction.RecordView.class);

        assertEquals("乾坤一掷", execution.params().get("server"));
        assertEquals("狐金", execution.params().get("name"));
        assertEquals("近期成交参考", view.context());
        assertRecordExcludes(view, "id", "url", "token");
    }

    @Test
    void shouldUseCurrentGoldPlatformsOnly() {
        TradeDemonData price = new TradeDemonData();
        price.setId(7);
        price.setZone("电信区");
        price.setServer("乾坤一掷");
        price.setTieba("0.52");
        price.setWanbaolou("0.55");
        price.setDd373("0.53");
        price.setUu898("legacy");
        price.setFive173("legacy");
        price.setSeven881("legacy");
        price.setDate("2026-07-15");

        Execution execution = execute(REGEX.TradeDemon, "金价 乾坤一掷", List.of(price));
        TradeDemonAction.PriceView view = firstView(execution.response(), TradeDemonAction.PriceView.class);

        assertEquals("0.55", view.wanbaolou());
        assertRecordExcludes(view, "id", "uu898", "five173", "seven881", "time");
    }

    @Test
    void shouldReadCurrentMechContract() throws Exception {
        OfficialQueryData.MechCalculator data = new ObjectMapper().readValue("""
                {"curr":{"node":"乾位","data":"先点亮左侧机关"},
                 "next":{"node":"坎位","data":"再触发中央石灯"},
                 "time":"2026-07-15 15:00:00","cdtn":"完成当前机关后切换"}
                """, OfficialQueryData.MechCalculator.class);

        Execution execution = execute(REGEX.MechCalculator, "副本解密", data);
        MechCalculatorAction.MechView view = assertInstanceOf(
                MechCalculatorAction.MechView.class, template(execution.response()).get("data"));

        assertEquals("乾位", view.current().node());
        assertEquals("再触发中央石灯", view.next().result());
        assertEquals("完成当前机关后切换", view.condition());
        assertRecordExcludes(view, "nowTime", "nowNode", "nowResult", "nextNode", "nextResult", "intervalTime");
    }

    @Test
    void shouldRemoveDuowanIdsAndLogoFromTemplateView() {
        OfficialQueryData.DuowanChannel channel = new OfficialQueryData.DuowanChannel();
        channel.setSid(123L);
        channel.setLogoUrl("https://example.invalid/logo.png");
        channel.setUsers(186);
        channel.setSnick("浩气盟统战");
        channel.setLimit(500);
        channel.setLogo(9);
        channel.setAsid(8);
        channel.setEsid("internal");
        channel.setCampName("浩气盟");
        OfficialQueryData.DuowanStatistics server = new OfficialQueryData.DuowanStatistics();
        server.setServer("乾坤一掷");
        server.setData(List.of(channel));

        Execution execution = execute(REGEX.DuowanStatistics, "统战 乾坤一掷", List.of(server));
        DuowanStatisticsAction.ServerView group = firstView(execution.response(), DuowanStatisticsAction.ServerView.class);
        DuowanStatisticsAction.ChannelView view = group.channels().get(0);

        assertEquals("浩气盟统战", view.name());
        assertEquals(186, view.users());
        assertRecordExcludes(view, "sid", "logoUrl", "logo", "asid", "esid");
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
            case TiebaItemRecords -> new TiebaItemRecordsAction(properties, requestUtil, configurationService);
            case TradeDemon -> new TradeDemonAction(properties, requestUtil, configurationService);
            case MechCalculator -> new MechCalculatorAction(properties, requestUtil, configurationService);
            case DuowanStatistics -> new DuowanStatisticsAction(properties, requestUtil, configurationService);
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
