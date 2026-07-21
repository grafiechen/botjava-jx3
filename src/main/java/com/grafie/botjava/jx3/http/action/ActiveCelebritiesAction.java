package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.data.active.ActiveCelebritiesData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;


/**
 * 行侠事件
 * @author grafie.chen
 * @since 2025/1/22  17:16
 */
@Jx3Action
public class ActiveCelebritiesAction extends Jx3BaseAction {

    public ActiveCelebritiesAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupConfigurationService groupConfigurationService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
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
        return "行侠事件";
    }

    @Override
    protected Map<String, Object> buildTemplateData(BaseResult baseResult) {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("name", currentArguments().get("name"));
        template.put("data", toViews(baseResult == null ? null : baseResult.getData()));
        return template;
    }

    private List<EventView> toViews(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        List<EventView> views = new ArrayList<>();
        for (Object item : values) {
            if (item instanceof ActiveCelebritiesData event) {
                views.add(new EventView(event.getMapName(), event.getEvent(), event.getSite(),
                        event.getDesc(), event.getTime()));
            }
        }
        return List.copyOf(views);
    }

    public record EventView(String mapName, String event, String site, String description, String time) {
    }
}
