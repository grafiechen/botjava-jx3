package com.grafie.botjava.service;

import com.grafie.botjava.entity.UserInfo;
import com.grafie.botjava.entity.UserRoleBinding;
import com.grafie.botjava.entity.dto.common.AuthorDto;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.http.command.CommandArguments;
import com.grafie.botjava.mapper.UserInfoMapper;
import com.grafie.botjava.mapper.UserRoleBindingMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserCommandPreferenceServiceTest {

    @Test
    void shouldPersistRoleAndDefaultByMemberOpenId() {
        Fixture fixture = fixture();

        UserInfo saved = fixture.service.bind(message("account-1"), " 乾坤一掷 ", " 加菲 ", " 万花 ");

        ArgumentCaptor<UserRoleBinding> roleCaptor = ArgumentCaptor.forClass(UserRoleBinding.class);
        verify(fixture.roles).save(roleCaptor.capture());
        assertEquals("group-1", roleCaptor.getValue().getGroupOpenId());
        assertEquals("account-1", roleCaptor.getValue().getMemberOpenId());
        assertEquals("乾坤一掷", roleCaptor.getValue().getServer());
        assertEquals("加菲", roleCaptor.getValue().getRoleName());
        assertEquals("万花", roleCaptor.getValue().getSchool());
        assertEquals("乾坤一掷", saved.getServer());
        assertEquals("加菲", saved.getRoleName());
        assertEquals("万花", saved.getSchool());
    }

    @Test
    void shouldAddAnotherRoleWithoutReplacingExistingDefault() {
        Fixture fixture = fixture();
        when(fixture.users.findByMemberOpenId("account-1")).thenReturn(userInfo("乾坤一掷", "加菲", "万花"));

        fixture.service.addRole(message("account-1"), "梦江南", "乔峰", "丐帮");

        verify(fixture.roles).save(any(UserRoleBinding.class));
        verify(fixture.users, never()).save(any(UserInfo.class));
    }

    @Test
    void shouldUpdateSavedRoleSchoolByServerAndRoleName() {
        Fixture fixture = fixture();
        UserRoleBinding role = role("account-1", "梦江南", "乔峰", "丐帮");
        role.setId(7L);
        when(fixture.roles.findByGroupOpenIdAndMemberOpenIdAndServerAndRoleName("group-1", "account-1", "梦江南", "乔峰"))
                .thenReturn(role);

        UserRoleBinding updated = fixture.service.updateRoleSchool(
                message("account-1"), "梦江南", "乔峰", "万花");
        assertEquals("梦江南", updated.getServer());
        assertEquals("乔峰", updated.getRoleName());
        assertEquals("万花", updated.getSchool());

        assertThrows(IllegalArgumentException.class,
                () -> fixture.service.updateRoleSchool(
                        message("account-2"), "梦江南", "乔峰", "万花"));
    }

    @Test
    void shouldPromoteOldestRemainingRoleWhenDefaultIsRemoved() {
        Fixture fixture = fixture();
        UserInfo current = userInfo("乾坤一掷", "加菲", "万花");
        UserRoleBinding removed = role("account-1", "乾坤一掷", "加菲", "万花");
        removed.setId(1L);
        UserRoleBinding remaining = role("account-1", "梦江南", "乔峰", "丐帮");
        remaining.setId(2L);
        when(fixture.users.findByMemberOpenId("account-1")).thenReturn(current);
        when(fixture.roles.findByGroupOpenIdAndMemberOpenIdAndServerAndRoleName("group-1", "account-1", "乾坤一掷", "加菲"))
                .thenReturn(removed);
        when(fixture.roles.findByGroupOpenIdAndMemberOpenIdOrderByIdAsc("group-1", "account-1")).thenReturn(List.of(remaining));

        assertTrue(fixture.service.deleteRole(message("account-1"), "乾坤一掷", "加菲"));
        verify(fixture.roles).delete(removed);
        ArgumentCaptor<UserInfo> defaultCaptor = ArgumentCaptor.forClass(UserInfo.class);
        verify(fixture.users).save(defaultCaptor.capture());
        assertEquals("梦江南", defaultCaptor.getValue().getServer());
        assertEquals("乔峰", defaultCaptor.getValue().getRoleName());
        assertEquals("丐帮", defaultCaptor.getValue().getSchool());
    }

    @Test
    void shouldNotExposeDefaultRoleWithoutGroupBinding() {
        Fixture fixture = fixture();
        when(fixture.users.findByMemberOpenId("account-1")).thenReturn(userInfo("乾坤一掷", "加菲", "万花"));
        when(fixture.roles.findByGroupOpenIdAndMemberOpenIdOrderByIdAsc("group-1", "account-1")).thenReturn(List.of());

        UserCommandPreferenceService.BindingSnapshot snapshot = fixture.service.findBindings(message("account-1"));
        assertTrue(snapshot.roles().isEmpty());
        verify(fixture.users).findByMemberOpenId("account-1");
        verify(fixture.users, never()).findByMemberOpenId("account-2");
    }

    @Test
    void shouldApplyPersonalDefaultsAndKeepExplicitCommandValues() {
        Fixture fixture = fixture();
        UserInfo preferences = userInfo("乾坤一掷", "绑定角色", "万花");
        when(fixture.users.findByMemberOpenId("account-1")).thenReturn(preferences);

        CommandArguments defaults = fixture.service.applyDefaults(message("account-1"), CommandArguments.of(Map.of()));
        assertEquals("乾坤一掷", defaults.server(null));
        assertEquals("绑定角色", defaults.roleName());
        assertEquals("万花", defaults.school());

        CommandArguments explicit = fixture.service.applyDefaults(message("account-1"),
                CommandArguments.of(Map.of("server", "梦江南", "value", "本次角色", "school", "七秀")));
        assertEquals("梦江南", explicit.server(null));
        assertEquals("本次角色", explicit.roleName());
        assertEquals("七秀", explicit.school());
    }

    @Test
    void shouldDeleteAllRolesOnlyForCurrentAccount() {
        Fixture fixture = fixture();
        UserInfo userInfo = userInfo("乾坤一掷", "加菲");
        when(fixture.users.findByMemberOpenId("account-1")).thenReturn(userInfo);
        when(fixture.roles.deleteByGroupOpenIdAndMemberOpenId("group-1", "account-1")).thenReturn(2L);

        assertTrue(fixture.service.unbind(message("account-1")));
        verify(fixture.users).delete(userInfo);
        verify(fixture.roles).deleteByGroupOpenIdAndMemberOpenId("group-1", "account-1");

        when(fixture.users.findByMemberOpenId("account-2")).thenReturn(null);
        assertFalse(fixture.service.unbind(message("account-2")));
    }

    @Test
    void shouldLeaveArgumentsUntouchedWhenMessageHasNoAccount() {
        Fixture fixture = fixture();
        CommandArguments arguments = CommandArguments.of(Map.of("value", "角色名"));

        assertSame(arguments, fixture.service.applyDefaults(new GroupAtMessageCreateDto(), arguments));
    }

    private static Fixture fixture() {
        UserInfoMapper users = mock(UserInfoMapper.class);
        UserRoleBindingMapper roles = mock(UserRoleBindingMapper.class);
        when(users.save(any(UserInfo.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roles.save(any(UserRoleBinding.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return new Fixture(users, roles, new UserCommandPreferenceService(users, roles));
    }

    private static UserInfo userInfo(String server, String roleName) {
        return userInfo(server, roleName, null);
    }

    private static UserInfo userInfo(String server, String roleName, String school) {
        UserInfo userInfo = new UserInfo();
        userInfo.setServer(server);
        userInfo.setRoleName(roleName);
        userInfo.setSchool(school);
        return userInfo;
    }

    private static UserRoleBinding role(String account, String server, String roleName) {
        return role(account, server, roleName, null);
    }

    private static UserRoleBinding role(String account, String server, String roleName, String school) {
        UserRoleBinding role = new UserRoleBinding();
        role.setGroupOpenId("group-1");
        role.setMemberOpenId(account);
        role.setServer(server);
        role.setRoleName(roleName);
        role.setSchool(school);
        return role;
    }

    private static GroupAtMessageCreateDto message(String memberOpenId) {
        AuthorDto author = new AuthorDto();
        author.setMemberOpenid(memberOpenId);
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setAuthor(author);
        message.setGroupOpenid("group-1");
        return message;
    }

    private record Fixture(UserInfoMapper users, UserRoleBindingMapper roles,
                           UserCommandPreferenceService service) {
    }
}
