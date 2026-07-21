package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.data.luck.LuckAdventureData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Jx3Action
public class LuckRecentAction extends Jx3BaseAction {
    public LuckRecentAction(ApiProperties apiProperties, Jx3RequestUtil requestUtil, GroupConfigurationService groupConfigurationService) {
        super(apiProperties, requestUtil, groupConfigurationService);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        return Map.of("server", currentArguments().server(getDefaultServer()));
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
        return "近期奇遇";
    }

    @Override
    protected Map<String, Object> buildTemplateData(BaseResult baseResult) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("server", currentArguments().server(getDefaultServer()));
        data.put("data", toViews(baseResult == null ? null : baseResult.getData()));
        return data;
    }

    private List<RecentView> toViews(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        List<RecentView> views = new ArrayList<>();
        for (Object item : values) {
            if (item instanceof LuckAdventureData recent) {
                views.add(new RecentView(
                        recent.getZone(), recent.getServer(), recent.getName(), recent.getEvent(), recent.getTime()));
            }
        }
        return List.copyOf(views);
    }

    public record RecentView(String zone, String server, String name, String event, String time) {
    }
}
