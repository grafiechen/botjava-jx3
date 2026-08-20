package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.AppearanceNameAliasService;
import com.grafie.botjava.service.GroupConfigurationService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppearanceNameAliasActionTest {

    private final AppearanceNameAliasService aliasService = mock(AppearanceNameAliasService.class);
    private final AppearanceNameAliasAction action = new AppearanceNameAliasAction(
            new ApiProperties(), mock(Jx3RequestUtil.class), mock(GroupConfigurationService.class), aliasService);

    @Test
    void shouldParseAppearanceNameCommands() {
        assertEquals(REGEX.AppearanceNameAliasAdd, REGEX.matchEnum("外观名称追加 金发·因陀罗 猴金 后进"));
        assertEquals(REGEX.AppearanceNameAliasList, REGEX.matchEnum("/外观名称列表"));
        assertEquals(REGEX.AppearanceNameAliasQuery, REGEX.matchEnum("外观名称查询 猴金"));
        assertEquals(REGEX.AppearanceNameAliasPendingList, REGEX.matchEnum("外观名称待审核列表"));
        assertEquals(REGEX.AppearanceNameAliasApprove, REGEX.matchEnum("外观名称审核 金发·因陀罗 猴金"));
        assertEquals(REGEX.AppearanceNameAliasDelete, REGEX.matchEnum("外观名称删除 金发·因陀罗 猴金"));
        assertEquals(REGEX.AppearanceNameAliasDelete, REGEX.matchEnum("外观名称删除 猴金"));
        assertEquals(REGEX.CommandAccess.PUBLIC, REGEX.AppearanceNameAliasPendingList.getCommandAccess());
        assertEquals(REGEX.CommandAccess.PUBLIC, REGEX.AppearanceNameAliasApprove.getCommandAccess());
        assertEquals(REGEX.CommandAccess.PUBLIC, REGEX.AppearanceNameAliasDelete.getCommandAccess());
        assertNull(REGEX.matchEnum("外观名称追加 金发·因陀罗"));
    }

    @Test
    void shouldRenderAddResultAsImmediatelyAvailable() {
        GroupAtMessageCreateDto message = message();
        when(aliasService.add(message, "金发·因陀罗", "猴金 后进", REGEX.AppearanceNameAliasAdd))
                .thenReturn(AppearanceNameAliasService.AddResult.added(
                        "金发·因陀罗", List.of("猴金", "后进"), List.of()));

        BotResponse response = action.doRequest(
                message, "外观名称追加 金发·因陀罗 猴金 后进", REGEX.AppearanceNameAliasAdd);

        assertEquals("外观名称“金发·因陀罗”已新增：猴金、后进。", response.getContent());
    }

    @Test
    void shouldRenderApprovedListAndQueryResult() {
        when(aliasService.list("group-1")).thenReturn(List.of(
                new AppearanceNameAliasService.AliasGroup("金发·因陀罗", List.of("猴金", "后进"))
        ));
        when(aliasService.lookup("group-1", "猴金"))
                .thenReturn(AppearanceNameAliasService.LookupResult.found(
                        "猴金", "猴金",
                        new AppearanceNameAliasService.AliasGroup("金发·因陀罗", List.of("猴金", "后进"))));

        BotResponse list = action.doRequest(message(), "外观名称列表", REGEX.AppearanceNameAliasList);
        BotResponse query = action.doRequest(message(), "/外观名称查询 猴金", REGEX.AppearanceNameAliasQuery);

        assertEquals("全局已审核外观名称列表：\n1. 金发·因陀罗：猴金、后进", list.getContent());
        assertEquals("金发·因陀罗：猴金、后进", query.getContent());
    }

    @Test
    void shouldRenderPendingListAndApproveResult() {
        GroupAtMessageCreateDto message = message();
        when(aliasService.pendingList(message, REGEX.AppearanceNameAliasPendingList))
                .thenReturn(AppearanceNameAliasService.PendingListResult.allowed(List.of(
                        new AppearanceNameAliasService.AliasGroup("金发·因陀罗", List.of("猴金"))
                )));
        when(aliasService.approve(message, "金发·因陀罗", "猴金", REGEX.AppearanceNameAliasApprove))
                .thenReturn(AppearanceNameAliasService.ReviewResult.approved("金发·因陀罗", 1));

        BotResponse pending = action.doRequest(message, "外观名称待审核列表", REGEX.AppearanceNameAliasPendingList);
        BotResponse approved = action.doRequest(message, "外观名称审核 金发·因陀罗 猴金", REGEX.AppearanceNameAliasApprove);

        assertEquals("全局待审核外观名称列表：\n1. 金发·因陀罗：猴金", pending.getContent());
        assertEquals("外观名称“金发·因陀罗”已审核通过 1 个别名。", approved.getContent());
    }

    @Test
    void shouldRenderDeleteResult() {
        GroupAtMessageCreateDto message = message();
        when(aliasService.delete(message, "金发·因陀罗", "猴金", REGEX.AppearanceNameAliasDelete))
                .thenReturn(AppearanceNameAliasService.DeleteResult.aliasDeleted("金发·因陀罗", "猴金"));

        BotResponse response = action.doRequest(
                message, "外观名称删除 金发·因陀罗 猴金", REGEX.AppearanceNameAliasDelete);

        assertEquals("外观名称“金发·因陀罗”的别名“猴金”已删除。", response.getContent());
    }

    private GroupAtMessageCreateDto message() {
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setId("message-1");
        message.setGroupOpenid("group-1");
        return message;
    }
}
