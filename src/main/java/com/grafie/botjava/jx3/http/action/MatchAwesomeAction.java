package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.BaseResult;

import java.util.LinkedHashMap;
import java.util.Map;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;


/**
 * 名剑排行
 * @author grafie.chen
 * @since 2025/1/22  17:20
 */
@Jx3Action
public class MatchAwesomeAction extends Jx3BaseAction {
    public MatchAwesomeAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupConfigurationService groupConfigurationService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        java.util.HashMap<String, Object> params = new java.util.HashMap<>();
        params.put("mode", currentArguments().integerOneOf("mode", 22, 33, 55).orElse(33));
        currentArguments().integer("limit", 1, 100).ifPresent(limit -> params.put("limit", limit));
        params.put("ticket", apiProperties.getTicket());
        return params;
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
        return "名剑排行";
    }

    @Override
    protected Object buildTemplateData(BaseResult baseResult) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("mode", currentArguments().integerOneOf("mode", 22, 33, 55).orElse(33));
        data.put("data", baseResult == null ? null : baseResult.getData());
        return data;
    }
}
