package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.BaseResult;

import java.util.Map;
import java.util.LinkedHashMap;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;

/**
 * 维护公告
 * @author grafie.chen
 * @since 2025/1/22  17:18
 */
@Jx3Action
public class NewsAnnounceAction extends Jx3BaseAction {
    public NewsAnnounceAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupConfigurationService groupConfigurationService) {
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
        return "新闻资讯";
    }

    @Override
    protected Map<String, Object> buildTemplateData(BaseResult baseResult) {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("title", "维护公告");
        template.put("data", NewsViewSupport.toViews(baseResult == null ? null : baseResult.getData()));
        return template;
    }
}
