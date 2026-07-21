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
import com.grafie.botjava.util.TimeUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Jx3Action
public class ActiveNextEventAction extends Jx3BaseAction {
    public ActiveNextEventAction(ApiProperties apiProperties, Jx3RequestUtil requestUtil, GroupConfigurationService groupConfigurationService) {
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
        return "扶摇预测";
    }

    @Override
    protected Map<String, Object> buildTemplateData(BaseResult baseResult) {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("server", currentArguments().server(getDefaultServer()));
        template.put("data", toViews(baseResult == null ? null : baseResult.getData()));
        return template;
    }

    private List<EventView> toViews(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        List<EventView> views = new ArrayList<>();
        for (Object item : values) {
            if (item instanceof OfficialQueryData.ActiveNextEvent event) {
                views.add(new EventView(event.getZone(), event.getServer(),
                        event.getTime() == null ? "" : TimeUtils.timeFormatting(event.getTime())));
            }
        }
        return List.copyOf(views);
    }

    public record EventView(String zone, String server, String time) {
    }
}
