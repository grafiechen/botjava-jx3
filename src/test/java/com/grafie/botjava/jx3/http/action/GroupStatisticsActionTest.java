package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import com.grafie.botjava.service.CommandInvocationQueryService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroupStatisticsActionTest {

    @Test
    void shouldRenderGroupStatistics() {
        CommandInvocationQueryService queryService = mock(CommandInvocationQueryService.class);
        when(queryService.statistics("group-1", 30)).thenReturn(
                new CommandInvocationQueryService.Statistics(30, List.of(
                        new CommandInvocationQueryService.CommandStat("开服状态", 12, 9, 124))));

        BotResponse response = action(queryService).doRequest(message(), "群统计 30", REGEX.GroupStatistics);

        assertEquals("近30天群指令统计：\n开服状态：12次，成功75%，平均124ms", response.getContent());
        verify(queryService).statistics("group-1", 30);
    }

    @Test
    void shouldRenderEmptyStatisticsWithDefaultWindow() {
        CommandInvocationQueryService queryService = mock(CommandInvocationQueryService.class);
        when(queryService.statistics("group-1", null)).thenReturn(
                new CommandInvocationQueryService.Statistics(7, List.of()));

        BotResponse response = action(queryService).doRequest(message(), "群统计", REGEX.GroupStatistics);

        assertEquals("近7天暂无群指令调用记录。", response.getContent());
    }

    private static GroupStatisticsAction action(CommandInvocationQueryService queryService) {
        return new GroupStatisticsAction(new ApiProperties(), mock(Jx3RequestUtil.class),
                mock(GroupConfigurationService.class), queryService);
    }

    private static GroupAtMessageCreateDto message() {
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setGroupOpenid("group-1");
        return message;
    }
}
