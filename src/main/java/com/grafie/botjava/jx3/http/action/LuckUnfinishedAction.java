package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.data.luck.unfinished.LuckUnfinishedData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;


/**
 * 未出奇遇
 * @author grafie.chen
 * @since 2025/1/22  17:32
 */
@Jx3Action
public class LuckUnfinishedAction extends Jx3BaseAction {
    public LuckUnfinishedAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupConfigurationService groupConfigurationService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        return Map.of(
                "server", currentArguments().server(getDefaultServer()),
                "name", currentArguments().roleName()
        );
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
        return "未做奇遇";
    }

    @Override
    protected Map<String, Object> buildTemplateData(BaseResult baseResult) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("server", currentArguments().server(getDefaultServer()));
        data.put("name", currentArguments().roleName());
        data.put("data", toViews(baseResult == null ? null : baseResult.getData()));
        return data;
    }

    private List<UnfinishedView> toViews(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        List<UnfinishedView> views = new ArrayList<>();
        for (Object item : values) {
            if (item instanceof LuckUnfinishedData unfinished) {
                views.add(new UnfinishedView(unfinished.getName(), unfinished.getType(), unfinished.getLevel()));
            }
        }
        return List.copyOf(views);
    }

    public record UnfinishedView(String name, String type, Integer level) {
    }
}
