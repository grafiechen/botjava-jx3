package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import com.grafie.botjava.service.GroupRequirementService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroupRequirementActionTest {

    private final GroupRequirementService requirementService = mock(GroupRequirementService.class);
    private final GroupRequirementAction action = new GroupRequirementAction(
            new ApiProperties(), mock(Jx3RequestUtil.class),
            mock(GroupConfigurationService.class), requirementService);

    @Test
    void shouldParseRequirementCommands() {
        assertEquals(REGEX.GroupRequirementCreate, REGEX.matchEnum("创建需求 奶花配装"));
        assertEquals(REGEX.GroupRequirementAppend, REGEX.matchEnum("/追加需求 奶花配装 我可以做"));
        assertEquals(REGEX.GroupRequirementRename, REGEX.matchEnum("修改需求 奶花配装 奶秀配装"));
        assertEquals(REGEX.GroupRequirementStatus, REGEX.matchEnum("需求状态 奶花配装 acb 完成"));
        assertEquals(REGEX.GroupRequirementCancel, REGEX.matchEnum("取消需求 奶花配装 1"));
        assertNull(REGEX.matchEnum("取消需求 奶花配装"));
        assertEquals(REGEX.GroupRequirementDelete, REGEX.matchEnum("删除需求 奶花配装"));
        assertEquals(REGEX.GroupRequirementDetail, REGEX.matchEnum("需求详情 奶花配装"));
        assertTrue(REGEX.GroupRequirementCreate.getCommandAccess().allows("member"));
    }

    @Test
    void shouldRenderRequirementList() {
        when(requirementService.list("group-1")).thenReturn(List.of(
                new GroupRequirementService.RequirementSummary("奶花配装", 2),
                new GroupRequirementService.RequirementSummary("开荒安排", 0)
        ));

        BotResponse response = action.doRequest(message(), "需求列表", REGEX.GroupRequirementList);

        assertEquals("本群需求列表：\n1. 奶花配装（2条）\n2. 开荒安排（0条）", response.getContent());
    }

    @Test
    void shouldRenderAppendAndCancelResult() {
        GroupAtMessageCreateDto message = message();
        when(requirementService.append(message, "奶花配装", "我可以做"))
                .thenReturn(new GroupRequirementService.AppendResult(true, "奶花配装", 1));
        when(requirementService.cancelOwnItem(message, "奶花配装", 1))
                .thenReturn(GroupRequirementService.CancelResult.canceled("奶花配装", 1));

        BotResponse appended = action.doRequest(message, "追加需求 奶花配装 我可以做", REGEX.GroupRequirementAppend);
        BotResponse canceled = action.doRequest(message, "取消需求 奶花配装 1", REGEX.GroupRequirementCancel);

        assertEquals("已追加到需求“奶花配装”，当前共 1 条。", appended.getContent());
        assertEquals("已取消需求“奶花配装”下第 1 条追加内容。", canceled.getContent());
    }

    @Test
    void shouldRenderOwnerOnlyRenameAndDeleteResult() {
        GroupAtMessageCreateDto message = message();
        when(requirementService.rename(message, "奶花配装", "奶秀配装"))
                .thenReturn(GroupRequirementService.RenameResult.renamed("奶花配装", "奶秀配装"));
        when(requirementService.updateStatus(message, "奶秀配装", "acb", "完成"))
                .thenReturn(GroupRequirementService.StatusResult.updated("奶秀配装", "acb", "完成"));
        when(requirementService.deleteOwnRequirement(message, "奶秀配装"))
                .thenReturn(GroupRequirementService.DeleteResult.deleted("奶秀配装"));

        BotResponse renamed = action.doRequest(message, "修改需求 奶花配装 奶秀配装", REGEX.GroupRequirementRename);
        BotResponse updatedStatus = action.doRequest(message, "需求状态 奶秀配装 acb 完成", REGEX.GroupRequirementStatus);
        BotResponse deleted = action.doRequest(message, "删除需求 奶秀配装", REGEX.GroupRequirementDelete);

        assertEquals("需求“奶花配装”已修改为“奶秀配装”。", renamed.getContent());
        assertEquals("需求“奶秀配装”下“acb”的状态已更新为：“完成”。", updatedStatus.getContent());
        assertEquals("需求“奶秀配装”已删除。", deleted.getContent());
        verify(requirementService).rename(message, "奶花配装", "奶秀配装");
        verify(requirementService).updateStatus(message, "奶秀配装", "acb", "完成");
        verify(requirementService).deleteOwnRequirement(message, "奶秀配装");
    }

    private GroupAtMessageCreateDto message() {
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setId("message-1");
        message.setGroupOpenid("group-1");
        return message;
    }
}
