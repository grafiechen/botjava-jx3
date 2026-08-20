package com.grafie.botjava.service;

import com.grafie.botjava.entity.GroupRequirement;
import com.grafie.botjava.entity.GroupRequirementItem;
import com.grafie.botjava.entity.dto.common.AuthorDto;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.mapper.GroupRequirementItemMapper;
import com.grafie.botjava.mapper.GroupRequirementMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroupRequirementServiceTest {

    private final GroupRequirementMapper requirementMapper = mock(GroupRequirementMapper.class);
    private final GroupRequirementItemMapper itemMapper = mock(GroupRequirementItemMapper.class);
    private final GroupRequirementService service = new GroupRequirementService(requirementMapper, itemMapper);

    @Test
    void shouldAllowAdminToRenameRequirementCreatedByOtherMember() {
        GroupRequirement requirement = requirement("member-owner");
        when(requirementMapper.findByGroupOpenIdAndTitle("group-1", "奶花配装"))
                .thenReturn(requirement);

        GroupRequirementService.RenameResult result = service.rename(
                message("member-admin", "admin"), "奶花配装", "奶秀配装");

        assertEquals(GroupRequirementService.RenameResult.Status.RENAMED, result.status());
        assertEquals("奶秀配装", requirement.getTitle());
        verify(requirementMapper).save(requirement);
    }

    @Test
    void shouldAllowAdminToUpdateRequirementStatusCreatedByOtherMember() {
        GroupRequirement requirement = requirement("member-owner");
        GroupRequirementItem item = item(1L, "member-other");
        item.setContent("acb");
        when(requirementMapper.findByGroupOpenIdAndTitle("group-1", "奶花配装"))
                .thenReturn(requirement);
        when(itemMapper.findByRequirement_IdOrderByCreateTimeAscIdAsc(7L))
                .thenReturn(List.of(item));

        GroupRequirementService.StatusResult result = service.updateStatus(
                message("member-admin", "admin"), "奶花配装", "acb", "完成");

        assertEquals(GroupRequirementService.StatusResult.Status.UPDATED, result.status());
        assertEquals("完成", item.getStatus());
        verify(itemMapper).save(item);
    }

    @Test
    void shouldRejectNormalMemberUpdatingRequirementStatusCreatedByOtherMember() {
        GroupRequirement requirement = requirement("member-owner");
        when(requirementMapper.findByGroupOpenIdAndTitle("group-1", "奶花配装"))
                .thenReturn(requirement);

        GroupRequirementService.StatusResult result = service.updateStatus(
                message("member-other", "member"), "奶花配装", "acb", "完成");

        assertEquals(GroupRequirementService.StatusResult.Status.NOT_OWNER, result.status());
        verify(itemMapper, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectUpdatingDuplicatedRequirementItemStatus() {
        GroupRequirement requirement = requirement("member-owner");
        GroupRequirementItem firstItem = item(1L, "member-one");
        firstItem.setContent("acb");
        GroupRequirementItem secondItem = item(2L, "member-two");
        secondItem.setContent("acb");
        when(requirementMapper.findByGroupOpenIdAndTitle("group-1", "奶花配装"))
                .thenReturn(requirement);
        when(itemMapper.findByRequirement_IdOrderByCreateTimeAscIdAsc(7L))
                .thenReturn(List.of(firstItem, secondItem));

        GroupRequirementService.StatusResult result = service.updateStatus(
                message("member-owner", "member"), "奶花配装", "acb", "完成");

        assertEquals(GroupRequirementService.StatusResult.Status.DUPLICATED, result.status());
        verify(itemMapper, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldAllowAdminToDeleteRequirementCreatedByOtherMember() {
        GroupRequirement requirement = requirement("member-owner");
        when(requirementMapper.findByGroupOpenIdAndTitle("group-1", "奶花配装"))
                .thenReturn(requirement);

        GroupRequirementService.DeleteResult result = service.deleteOwnRequirement(
                message("member-admin", "admin"), "奶花配装");

        assertEquals(GroupRequirementService.DeleteResult.Status.DELETED, result.status());
        verify(itemMapper).deleteByRequirement_Id(7L);
        verify(requirementMapper).delete(requirement);
    }

    @Test
    void shouldRejectNormalMemberDeletingRequirementCreatedByOtherMember() {
        GroupRequirement requirement = requirement("member-owner");
        when(requirementMapper.findByGroupOpenIdAndTitle("group-1", "奶花配装"))
                .thenReturn(requirement);

        GroupRequirementService.DeleteResult result = service.deleteOwnRequirement(
                message("member-other", "member"), "奶花配装");

        assertEquals(GroupRequirementService.DeleteResult.Status.NOT_OWNER, result.status());
        verify(itemMapper, never()).deleteByRequirement_Id(7L);
        verify(requirementMapper, never()).delete(requirement);
    }

    @Test
    void shouldCancelOwnRequirementItemByIndex() {
        GroupRequirement requirement = requirement("member-owner");
        GroupRequirementItem otherItem = item(1L, "member-other");
        GroupRequirementItem ownItem = item(2L, "member-self");
        when(requirementMapper.findByGroupOpenIdAndTitle("group-1", "奶花配装"))
                .thenReturn(requirement);
        when(itemMapper.findByRequirement_IdOrderByCreateTimeAscIdAsc(7L))
                .thenReturn(List.of(otherItem, ownItem));

        GroupRequirementService.CancelResult result = service.cancelOwnItem(
                message("member-self", "member"), "奶花配装", 2);

        assertEquals(GroupRequirementService.CancelResult.Status.CANCELED, result.status());
        verify(itemMapper).delete(ownItem);
    }

    @Test
    void shouldAllowRequirementCreatorToCancelRequirementItemCreatedByOtherMember() {
        GroupRequirement requirement = requirement("member-owner");
        GroupRequirementItem otherItem = item(1L, "member-other");
        when(requirementMapper.findByGroupOpenIdAndTitle("group-1", "奶花配装"))
                .thenReturn(requirement);
        when(itemMapper.findByRequirement_IdOrderByCreateTimeAscIdAsc(7L))
                .thenReturn(List.of(otherItem));

        GroupRequirementService.CancelResult result = service.cancelOwnItem(
                message("member-owner", "member"), "奶花配装", 1);

        assertEquals(GroupRequirementService.CancelResult.Status.CANCELED, result.status());
        verify(itemMapper).delete(otherItem);
    }

    @Test
    void shouldAllowAdminToCancelRequirementItemCreatedByOtherMember() {
        GroupRequirement requirement = requirement("member-owner");
        GroupRequirementItem otherItem = item(1L, "member-other");
        when(requirementMapper.findByGroupOpenIdAndTitle("group-1", "奶花配装"))
                .thenReturn(requirement);
        when(itemMapper.findByRequirement_IdOrderByCreateTimeAscIdAsc(7L))
                .thenReturn(List.of(otherItem));

        GroupRequirementService.CancelResult result = service.cancelOwnItem(
                message("member-admin", "admin"), "奶花配装", 1);

        assertEquals(GroupRequirementService.CancelResult.Status.CANCELED, result.status());
        verify(itemMapper).delete(otherItem);
    }

    @Test
    void shouldRejectCancelingRequirementItemCreatedByOtherMember() {
        GroupRequirement requirement = requirement("member-owner");
        GroupRequirementItem otherItem = item(1L, "member-other");
        when(requirementMapper.findByGroupOpenIdAndTitle("group-1", "奶花配装"))
                .thenReturn(requirement);
        when(itemMapper.findByRequirement_IdOrderByCreateTimeAscIdAsc(7L))
                .thenReturn(List.of(otherItem));

        GroupRequirementService.CancelResult result = service.cancelOwnItem(
                message("member-self", "member"), "奶花配装", 1);

        assertEquals(GroupRequirementService.CancelResult.Status.NOT_OWNER, result.status());
        verify(itemMapper, never()).delete(otherItem);
    }

    private GroupRequirement requirement(String creatorMemberOpenId) {
        GroupRequirement requirement = new GroupRequirement();
        requirement.setId(7L);
        requirement.setGroupOpenId("group-1");
        requirement.setTitle("奶花配装");
        requirement.setCreatorMemberOpenId(creatorMemberOpenId);
        return requirement;
    }

    private GroupRequirementItem item(Long id, String memberOpenId) {
        GroupRequirementItem item = new GroupRequirementItem();
        item.setId(id);
        item.setMemberOpenId(memberOpenId);
        return item;
    }

    private GroupAtMessageCreateDto message(String memberOpenId, String memberRole) {
        AuthorDto author = new AuthorDto();
        author.setMemberOpenid(memberOpenId);
        author.setMemberRole(memberRole);
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setGroupOpenid("group-1");
        message.setAuthor(author);
        return message;
    }
}
