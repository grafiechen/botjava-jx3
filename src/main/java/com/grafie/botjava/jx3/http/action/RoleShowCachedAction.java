package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;

import java.util.Map;
import java.util.LinkedHashMap;

@Jx3Action
public class RoleShowCachedAction extends Jx3BaseAction {
    public RoleShowCachedAction(ApiProperties apiProperties, Jx3RequestUtil requestUtil,
                                GroupConfigurationService groupConfigurationService) {
        super(apiProperties, requestUtil, groupConfigurationService);
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
        return "角色名片";
    }

    @Override
    protected Map<String, Object> buildTemplateData(BaseResult baseResult) {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("title", "缓存名片");
        template.put("server", currentArguments().server(getDefaultServer()));
        template.put("name", currentArguments().roleName());
        template.put("data", RoleCardTemplateSupport.toViews(
                baseResult == null ? null : baseResult.getData(), jx3RequestUtil));
        return template;
    }
}
