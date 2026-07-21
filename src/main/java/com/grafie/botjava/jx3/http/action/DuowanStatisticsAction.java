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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Jx3Action
public class DuowanStatisticsAction extends Jx3BaseAction {
    public DuowanStatisticsAction(ApiProperties apiProperties, Jx3RequestUtil requestUtil,
                                  GroupConfigurationService groupConfigurationService) {
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
        return "统战歪歪";
    }

    @Override
    protected Map<String, Object> buildTemplateData(BaseResult baseResult) {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("server", currentArguments().server(getDefaultServer()));
        template.put("data", toViews(baseResult == null ? null : baseResult.getData()));
        return template;
    }

    private List<ServerView> toViews(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        List<ServerView> views = new ArrayList<>();
        for (Object item : values) {
            if (item instanceof OfficialQueryData.DuowanStatistics server) {
                List<ChannelView> channels = server.getData() == null ? List.of() : server.getData().stream()
                        .filter(java.util.Objects::nonNull)
                        .map(channel -> new ChannelView(channel.getSnick(), channel.getCampName(),
                                channel.getUsers(), channel.getLimit()))
                        .toList();
                views.add(new ServerView(server.getServer(), channels));
            }
        }
        return List.copyOf(views);
    }

    public record ServerView(String server, List<ChannelView> channels) {
    }

    public record ChannelView(String name, String campName, Integer users, Integer limit) {
    }
}
