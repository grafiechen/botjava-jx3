package com.grafie.botjava.service;

import com.grafie.botjava.config.QqAdminProperties;
import com.grafie.botjava.entity.dto.common.AuthorDto;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.http.util.REGEX;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NoopAppearanceNamePermissionConfigurationTest {

    private final QqAdminProperties adminProperties = new QqAdminProperties();
    private final GroupCommandPermissionConfiguration permissionConfiguration = mock(GroupCommandPermissionConfiguration.class);
    private final NoopAppearanceNamePermissionConfiguration configuration =
            new NoopAppearanceNamePermissionConfiguration(adminProperties, permissionConfiguration);

    @Test
    void shouldAllowPublicAppearanceNameCommandsForNormalMembers() {
        assertTrue(configuration.canManageAppearanceName(message("member", "member-1"), REGEX.AppearanceNameAliasAdd));
        assertTrue(configuration.canManageAppearanceName(message("member", "member-1"), REGEX.AppearanceNameAliasList));
        assertTrue(configuration.canManageAppearanceName(message("member", "member-1"), REGEX.AppearanceNameAliasQuery));
    }

    @Test
    void shouldBypassAppearanceNameReviewForNow() {
        assertTrue(configuration.isAppearanceNameReviewPassed(
                message("member", "member-1"), REGEX.AppearanceNameAliasAdd));
    }

    @Test
    void shouldNotAllowGroupManagersToReviewOrDeleteAppearanceNameAliasByDefault() {
        assertFalse(configuration.canManageAppearanceName(message("member", "member-1"), REGEX.AppearanceNameAliasDelete));
        assertFalse(configuration.canManageAppearanceName(message("admin", "member-1"), REGEX.AppearanceNameAliasDelete));
        assertFalse(configuration.canManageAppearanceName(message("owner", "member-1"), REGEX.AppearanceNameAliasApprove));
    }

    @Test
    void shouldAllowConfiguredQqMasterToReviewAndDeleteAppearanceNameAlias() {
        adminProperties.setMasterOpenids(List.of("member-master"));

        assertTrue(configuration.canManageAppearanceName(
                message("member", "member-master"), REGEX.AppearanceNameAliasPendingList));
        assertTrue(configuration.canManageAppearanceName(
                message("admin", "member-master"), REGEX.AppearanceNameAliasApprove));
        assertTrue(configuration.canManageAppearanceName(
                message("owner", "member-master"), REGEX.AppearanceNameAliasDelete));
    }

    @Test
    void shouldAllowDatabasePermissionConfigurationToReviewAndDeleteAppearanceNameAlias() {
        when(permissionConfiguration.evaluate("group-1", "member-db", REGEX.AppearanceNameAliasApprove))
                .thenReturn(Optional.of(GroupCommandPermissionConfiguration.Decision.ALLOW));

        assertTrue(configuration.canManageAppearanceName(
                message("member", "member-db"), REGEX.AppearanceNameAliasApprove));
    }

    private GroupAtMessageCreateDto message(String memberRole, String memberOpenId) {
        AuthorDto author = new AuthorDto();
        author.setMemberRole(memberRole);
        author.setMemberOpenid(memberOpenId);
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setGroupOpenid("group-1");
        message.setAuthor(author);
        return message;
    }
}