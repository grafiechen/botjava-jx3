package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import com.grafie.botjava.service.CommandInvocationQueryService;

import java.util.Map;
import java.util.OptionalInt;

@Jx3Action
public class GroupStatisticsAction extends Jx3BaseAction {

    private final CommandInvocationQueryService queryService;

    public GroupStatisticsAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil,
                                 GroupConfigurationService groupConfigurationService,
                                 CommandInvocationQueryService queryService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
        this.queryService = queryService;
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        return Map.of();
    }

    @Override
    protected BotResponse dealAfterJx3ApiRequest(BaseResult baseResult) {
        OptionalInt requestedDays = currentArguments().integer("num", 1, Integer.MAX_VALUE);
        Integer days = requestedDays.isPresent() ? requestedDays.getAsInt() : null;
        CommandInvocationQueryService.Statistics statistics = queryService.statistics(
                currentMessage().getGroupOpenid(), days);
        if (statistics.rows().isEmpty()) {
            return BotResponse.text("近" + statistics.days() + "天暂无群指令调用记录。");
        }
        StringBuilder content = new StringBuilder("近")
                .append(statistics.days()).append("天群指令统计：");
        for (CommandInvocationQueryService.CommandStat row : statistics.rows()) {
            content.append("\n")
                    .append(row.displayName()).append("：")
                    .append(row.invocationCount()).append("次，成功")
                    .append(row.successRate()).append("%，平均")
                    .append(row.averageElapsedMillis()).append("ms");
        }
        return BotResponse.text(content.toString());
    }
}
