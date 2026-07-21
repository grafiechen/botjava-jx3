package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.data.server.ServerEventData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;

@Jx3Action

/**
 * 全服阵营事件
 * @author grafie.chen
 * @since 2025/1/22  17:23
 */
public class ServerEventAction extends Jx3BaseAction {
    public ServerEventAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupConfigurationService groupConfigurationService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        return Map.of();
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
        return "阵营事件";
    }

    @Override
    protected Map<String, Object> buildTemplateData(BaseResult baseResult) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("scope", "全服");
        data.put("data", toViews(baseResult == null ? null : baseResult.getData()));
        return data;
    }

    private List<EventView> toViews(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        List<EventView> views = new ArrayList<>();
        for (Object item : values) {
            if (item instanceof ServerEventData event) {
                views.add(new EventView(event.getCampName(), event.getFenxianName(),
                        event.getFriendName(), event.getRoleName(), event.getSeizeTime()));
            }
        }
        return List.copyOf(views);
    }

    public record EventView(String campName, String fenxianName, String friendName,
                            String roleName, String seizeTime) {
    }
}
