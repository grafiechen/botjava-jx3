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
 * 角色名片
 * 客户端的名片墙。
 * @author grafie.chen
 * @since 2025/1/22  17:29
 */
@Jx3Action
public class RoleShowRandomAction extends Jx3BaseAction {
    public RoleShowRandomAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupConfigurationService groupConfigurationService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        Map<String, Object> params = new LinkedHashMap<>();
        putIfPresent(params, "server", currentArguments().get("server"));
        putIfPresent(params, "body", currentArguments().get("body"));
        putIfPresent(params, "force", currentArguments().get("force"));
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
        return "角色名片";
    }

    @Override
    protected Map<String, Object> buildTemplateData(BaseResult baseResult) {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("title", "随机名片");
        template.put("server", currentArguments().get("server") == null ? "全服" : currentArguments().get("server"));
        template.put("filter", filterText());
        template.put("data", RoleCardTemplateSupport.toViews(
                baseResult == null ? null : baseResult.getData(), jx3RequestUtil));
        return template;
    }

    private String filterText() {
        String body = currentArguments().get("body");
        String force = currentArguments().get("force");
        if (body == null && force == null) {
            return "不限体型与门派";
        }
        return java.util.stream.Stream.of(body, force)
                .filter(value -> value != null && !value.isBlank())
                .collect(java.util.stream.Collectors.joining(" · "));
    }

    private void putIfPresent(Map<String, Object> params, String key, Object value) {
        if (value != null) {
            params.put(key, value);
        }
    }
}
