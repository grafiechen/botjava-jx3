package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.data.official.OfficialQueryData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Jx3Action
public class TradeItemSearchAction extends Jx3BaseAction {
    public TradeItemSearchAction(ApiProperties apiProperties, Jx3RequestUtil requestUtil,
                                 GroupConfigurationService groupConfigurationService) {
        super(apiProperties, requestUtil, groupConfigurationService);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        return Map.of("name", currentArguments().get("name"));
    }

    @Override
    protected BotResponse dealAfterJx3ApiRequest(BaseResult baseResult) {
        return buildMessageByTemplate(baseResult);
    }

    @Override
    protected BotResponse.ResponseType getResponseType() {
        return BotResponse.ResponseType.IMAGE;
    }

    @Override
    protected String getTemplatePath() {
        return "搜索物品";
    }

    @Override
    protected Map<String, Object> buildTemplateData(BaseResult baseResult) {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("name", currentArguments().get("name"));
        template.put("data", toViews(baseResult == null ? null : baseResult.getData()));
        return template;
    }

    private List<ItemView> toViews(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        List<ItemView> views = new ArrayList<>();
        Map<String, Optional<String>> images = new HashMap<>();
        for (Object item : values) {
            if (item instanceof OfficialQueryData.TradeItem tradeItem) {
                views.add(new ItemView(tradeItem.getCategory(), tradeItem.getSubclass(), tradeItem.getName(),
                        tradeItem.getAlias(), tradeItem.getValue(), tradeItem.getDesc(), tradeItem.getDate(),
                        image(tradeItem.getView(), images)));
            }
            if (views.size() >= 8) {
                break;
            }
        }
        return List.copyOf(views);
    }

    private String image(String url, Map<String, Optional<String>> images) {
        if (url == null || url.isBlank()) {
            return null;
        }
        return images.computeIfAbsent(url, jx3RequestUtil::loadRemoteImageDataUri).orElse(null);
    }

    public record ItemView(String category, String subclass, String name, String alias,
                           String value, String description, String date, String imageDataUri) {
    }
}
