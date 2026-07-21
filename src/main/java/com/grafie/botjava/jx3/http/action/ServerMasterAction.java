package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.data.server.ServerMasterData;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;

@Jx3Action

/**
 * 搜索区服
 *
 * @author grafie.chen
 * @since 2025/1/22  17:17
 */
public class ServerMasterAction extends Jx3BaseAction {
    public ServerMasterAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupConfigurationService groupConfigurationService) {
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
        return "搜索区服";
    }

    @Override
    protected Map<String, Object> buildTemplateData(BaseResult baseResult) {
        ServerMasterData data = baseResult != null && baseResult.getData() instanceof ServerMasterData value
                ? value : null;
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("query", currentArguments().get("name"));
        template.put("data", data == null ? null : new ServerView(
                data.getZone(), data.getName(), data.getCenter(), clean(data.getAlias()), clean(data.getSlave())));
        return template;
    }

    private List<String> clean(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream().filter(value -> value != null && !value.isBlank()).toList();
    }

    public record ServerView(String zone, String name, String center,
                             List<String> aliases, List<String> slaves) {
    }
}
