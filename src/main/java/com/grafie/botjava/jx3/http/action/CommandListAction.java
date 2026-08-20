package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Jx3Action
public class CommandListAction extends Jx3BaseAction {

    public CommandListAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil,
                             GroupConfigurationService groupConfigurationService) {
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
        return "指令列表";
    }

    @Override
    protected Object buildTemplateData(BaseResult baseResult) {
        Map<String, List<CommandView>> groupsByName = Arrays.stream(REGEX.values())
                .filter(REGEX::isShownInCommandList)
                .collect(LinkedHashMap::new,
                        (groups, definition) -> groups.computeIfAbsent(
                                definition.getCommandListGroup().getDisplayName(),
                                ignored -> new java.util.ArrayList<>()
                        ).add(commandView(definition)),
                        Map::putAll);
        List<GroupView> groups = groupsByName.entrySet().stream()
                .map(entry -> new GroupView(entry.getKey(), List.copyOf(entry.getValue())))
                .toList();
        int total = groups.stream().mapToInt(group -> group.commands().size()).sum();
        return new CommandListView("剑网 3 查询指令", total, groups);
    }

    private CommandView commandView(REGEX definition) {
        return new CommandView(
                definition.getDisplayName(),
                definition.getDescription(),
                definition.getExample(),
                accessText(definition),
                availabilityText(definition)
        );
    }

    private String accessText(REGEX definition) {
        return definition.getCommandAccess() == REGEX.CommandAccess.GROUP_ADMIN
                ? "群主/管理员" : "所有人";
    }

    private String availabilityText(REGEX definition) {
        return switch (definition.getCommandAvailability()) {
            case SYSTEM -> "系统";
            case PRODUCTION -> "正式";
            case EXPERIMENTAL -> "实验";
            case REVIEW -> "审核";
        };
    }

    public record CommandListView(String title, int total, List<GroupView> groups) {
    }

    public record GroupView(String name, List<CommandView> commands) {
    }

    public record CommandView(String name, String description, String example,
                              String access, String availability) {
    }
}
