package com.grafie.botjava.jx3.http.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.data.fraud.detail.DetailInfo;
import com.grafie.botjava.jx3.http.data.fraud.detail.FraudDetailData;
import com.grafie.botjava.jx3.http.data.news.NewsAllNewsData;
import com.grafie.botjava.jx3.http.data.news.NewsAnnounceData;
import com.grafie.botjava.jx3.http.data.official.OfficialQueryData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NewsFraudAndItemImageActionTest {

    private static final String SOURCE_URL = "https://www.jx3api.com/cache/item.png";
    private static final String DATA_URI = "data:image/png;base64,AA==";

    @Test
    void shouldReadCurrentNewsFieldsAndExposeOnlyReadableContent() throws Exception {
        NewsAllNewsData news = new ObjectMapper().readValue("""
                {"id":123,"catid":"5","type":"公告","title":"夏日版本更新",\
                 "date":"2026-07-15 07:30:00","url":"https://example.invalid/news/123"}
                """, NewsAllNewsData.class);

        Execution execution = execute(REGEX.NewsAllNews, "新闻 3", List.of(news));
        NewsItemView view = firstView(execution.response(), NewsItemView.class);

        assertEquals("新闻资讯", template(execution.response()).get("title"));
        assertEquals("公告", view.type());
        assertEquals("夏日版本更新", view.title());
        assertEquals("2026-07-15 07:30:00", view.date());
        assertEquals(3, execution.params().get("limit"));
        assertRecordExcludes(view, "id", "categoryId", "url");
    }

    @Test
    void shouldUseSameStableViewForMaintenanceAnnouncements() throws Exception {
        NewsAnnounceData announce = new ObjectMapper().readValue("""
                {"id":8,"catid":"2","type":"维护","title":"例行维护公告",\
                 "date":"2026-07-15 06:00:00","url":"https://example.invalid/announce/8"}
                """, NewsAnnounceData.class);

        Execution execution = execute(REGEX.NewsAnnounce, "更新公告", List.of(announce));
        NewsItemView view = firstView(execution.response(), NewsItemView.class);

        assertEquals("维护公告", template(execution.response()).get("title"));
        assertEquals("例行维护公告", view.title());
        assertRecordExcludes(view, "id", "categoryId", "url");
    }

    @Test
    void shouldFlattenFraudRecordsWithoutPostIdentifiers() {
        DetailInfo detail = new DetailInfo();
        detail.setTitle("公开避雷记录");
        detail.setTid(987654L);
        detail.setText("请在交易前再次核实相关信息。");
        detail.setTime(1_733_270_400L);
        FraudDetailData group = new FraudDetailData();
        group.setServer("乾坤一掷");
        group.setTieba("剑网3吧");
        group.setData(List.of(detail));

        Execution execution = execute(REGEX.FraudDetail, "骗子 570790267", List.of(group));
        FraudDetailAction.FraudView view = firstView(execution.response(), FraudDetailAction.FraudView.class);

        assertEquals("乾坤一掷", view.server());
        assertEquals("剑网3吧", view.tieba());
        assertEquals(570790267L, execution.params().get("uid"));
        assertRecordExcludes(view, "tid", "url");
    }

    @Test
    void shouldEmbedValidatedItemImageAndExcludeRawSearchFields() {
        OfficialQueryData.TradeItem item = new OfficialQueryData.TradeItem();
        item.setCategory("道具");
        item.setSubclass("节日物品");
        item.setName("十五夜观灯");
        item.setAlias("观灯");
        item.setWblalias("internal-alias");
        item.setValue("12000");
        item.setDesc("节日活动相关物品");
        item.setDate("2026-07-15");
        item.setView(SOURCE_URL);

        Execution execution = execute(REGEX.TradeItemSearch, "搜索物品 十五", List.of(item));
        TradeItemSearchAction.ItemView view = firstView(execution.response(), TradeItemSearchAction.ItemView.class);

        assertEquals("十五", template(execution.response()).get("name"));
        assertEquals(DATA_URI, view.imageDataUri());
        assertEquals("十五", execution.params().get("name"));
        assertRecordExcludes(view, "wblalias", "view", "url");
        verify(execution.requestUtil()).loadRemoteImageDataUri(SOURCE_URL);
    }

    private Execution execute(REGEX regex, String command, Object apiData) {
        Jx3RequestUtil requestUtil = mock(Jx3RequestUtil.class);
        RequestResult requestResult = new RequestResult();
        BaseResult<Object> baseResult = new BaseResult<>();
        baseResult.setCode(200);
        baseResult.setData(apiData);
        when(requestUtil.doPostRequest(anyString(), any())).thenReturn(requestResult);
        when(requestUtil.getResultRealData(requestResult, regex.getMethodEnum())).thenReturn(baseResult);
        when(requestUtil.loadRemoteImageDataUri(anyString())).thenReturn(Optional.of(DATA_URI));
        ApiProperties properties = new ApiProperties();
        properties.setDefaultServer("梦江南");
        Jx3BaseAction action = switch (regex) {
            case NewsAllNews -> new NewsAllNewsAction(properties, requestUtil, mock(GroupConfigurationService.class));
            case NewsAnnounce -> new NewsAnnounceAction(properties, requestUtil, mock(GroupConfigurationService.class));
            case FraudDetail -> new FraudDetailAction(properties, requestUtil, mock(GroupConfigurationService.class));
            case TradeItemSearch -> new TradeItemSearchAction(properties, requestUtil, mock(GroupConfigurationService.class));
            default -> throw new IllegalArgumentException("不支持的测试指令：" + regex);
        };

        BotResponse response = action.doRequest(message(), command, regex);
        assertEquals(BotResponse.ResponseType.IMAGE, response.getResponseType());
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(requestUtil).doPostRequest(anyString(), captor.capture());
        return new Execution(response, captor.getValue(), requestUtil);
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

    private record Execution(BotResponse response, Map<String, Object> params, Jx3RequestUtil requestUtil) {
    }
}
