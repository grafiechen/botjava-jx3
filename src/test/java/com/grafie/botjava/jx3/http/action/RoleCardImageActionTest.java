package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.data.role.RoleShowCardData;
import com.grafie.botjava.jx3.http.data.role.RoleShowRandomData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoleCardImageActionTest {

    private static final String SOURCE_URL = "https://www.jx3api.com/cache/card.png";
    private static final String DATA_URI = "data:image/png;base64,AA==";

    @Test
    void shouldBuildCurrentAndAllCardViewsFromValidatedImages() {
        RoleShowCardData card = card("夜温言", 2, true);

        Execution current = execute(REGEX.RoleShowCard, "名片 乾坤一掷 夜温言", card);
        Execution all = execute(REGEX.RoleShowCards, "所有名片 乾坤一掷 夜温言", List.of(card));

        assertEquals("角色名片", template(current.response()).get("title"));
        assertEquals("所有名片", template(all.response()).get("title"));
        RoleCardView view = firstView(current.response());
        assertEquals(DATA_URI, view.imageDataUri());
        assertEquals("夜温言", view.roleName());
        assertRecordExcludes(view, "showHash", "global", "url", "avatar");
        assertEquals("乾坤一掷", current.params().get("server"));
        assertEquals("夜温言", current.params().get("name"));
        verify(current.requestUtil()).loadRemoteImageDataUri(SOURCE_URL);
    }

    @Test
    void shouldBuildRandomCardWithoutExposingHashOrStatus() {
        RoleShowRandomData card = new RoleShowRandomData();
        card.setZone("电信区");
        card.setServer("唯我独尊");
        card.setName("山色暮相依");
        card.setShowHash("e70caa68eab3afc3732a70554f7661a5");
        card.setShowIndex(1);
        card.setAvatar(SOURCE_URL);
        card.setStatus(200);

        Execution execution = execute(REGEX.RoleShowRandom, "随机名片 唯我独尊 萝莉 万花", card);
        Map<?, ?> template = template(execution.response());
        RoleCardView view = firstView(execution.response());

        assertEquals("随机名片", template.get("title"));
        assertEquals("萝莉 · 万花", template.get("filter"));
        assertEquals(DATA_URI, view.imageDataUri());
        assertRecordExcludes(view, "showHash", "status", "avatar");
        assertEquals("萝莉", execution.params().get("body"));
        assertEquals("万花", execution.params().get("force"));
    }

    @Test
    void shouldBuildCachedCardAndLimitAllCardDownloads() {
        RoleShowCardData cached = card("夜温言", null, null);
        cached.setCache(1776228463L);
        Execution cachedExecution = execute(REGEX.RoleShowCached, "缓存名片 乾坤一掷 夜温言", cached);

        List<RoleShowCardData> cards = new ArrayList<>();
        for (int index = 0; index < 13; index++) {
            cards.add(card("角色" + index, index, index == 0));
        }
        Execution allExecution = execute(REGEX.RoleShowCards, "所有名片 乾坤一掷 夜温言", cards);

        assertEquals("缓存名片", template(cachedExecution.response()).get("title"));
        assertEquals(12, ((List<?>) template(allExecution.response()).get("data")).size());
        verify(allExecution.requestUtil(), times(1)).loadRemoteImageDataUri(SOURCE_URL);
    }

    private RoleShowCardData card(String name, Integer index, Boolean active) {
        RoleShowCardData card = new RoleShowCardData();
        card.setZone("电信区");
        card.setServer("乾坤一掷");
        card.setName(name);
        card.setGlobal("internal-hash");
        card.setShowIndex(index);
        card.setShowActive(active);
        card.setStaticUrl(SOURCE_URL);
        return card;
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
            case RoleShowCard, RoleShowCards -> new RoleShowCardAction(properties, requestUtil, mock(GroupConfigurationService.class));
            case RoleShowRandom -> new RoleShowRandomAction(properties, requestUtil, mock(GroupConfigurationService.class));
            case RoleShowCached -> new RoleShowCachedAction(properties, requestUtil, mock(GroupConfigurationService.class));
            default -> throw new IllegalArgumentException("不支持的测试指令：" + regex);
        };

        BotResponse response = action.doRequest(message(), command, regex);
        assertEquals(BotResponse.ResponseType.IMAGE, response.getResponseType());
        assertEquals("角色名片", response.getTemplateName());
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(requestUtil).doPostRequest(anyString(), captor.capture());
        return new Execution(response, captor.getValue(), requestUtil);
    }

    private Map<?, ?> template(BotResponse response) {
        return assertInstanceOf(Map.class, response.getTemplateData());
    }

    private RoleCardView firstView(BotResponse response) {
        return assertInstanceOf(RoleCardView.class, ((List<?>) template(response).get("data")).get(0));
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
