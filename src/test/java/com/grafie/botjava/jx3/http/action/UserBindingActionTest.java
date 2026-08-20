package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.UserInfo;
import com.grafie.botjava.entity.UserRoleBinding;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import com.grafie.botjava.service.UserCommandPreferenceService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserBindingActionTest {

    @Test
    void shouldBindCurrentAccount() {
        UserCommandPreferenceService preferences = mock(UserCommandPreferenceService.class);
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        when(preferences.bind(message, "乾坤一掷", "加菲", "万花"))
                .thenReturn(userInfo("乾坤一掷", "加菲", "万花"));

        BotResponse response = action(preferences).doRequest(
                message, "绑定角色 乾坤一掷 加菲 万花", REGEX.BindRole);

        assertEquals("默认角色已绑定：乾坤一掷 加菲 万花", response.getContent());
        verify(preferences).bind(message, "乾坤一掷", "加菲", "万花");
    }

    @Test
    void shouldAddAndModifyPersonalRole() {
        UserCommandPreferenceService preferences = mock(UserCommandPreferenceService.class);
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        when(preferences.addRole(message, "梦江南", "乔峰", "丐帮"))
                .thenReturn(role("梦江南", "乔峰", "丐帮"));
        when(preferences.updateRoleSchool(message, "梦江南", "乔峰", "万花"))
                .thenReturn(role("梦江南", "乔峰", "万花"));

        BotResponse added = action(preferences).doRequest(message, "添加角色 梦江南 乔峰 丐帮", REGEX.AddRole);
        BotResponse modified = action(preferences).doRequest(
                message, "修改角色 梦江南 乔峰 万花", REGEX.ModifyRole);

        assertEquals("常用角色已添加：梦江南 乔峰 丐帮", added.getContent());
        assertEquals("角色门派已修改：梦江南 乔峰 万花", modified.getContent());
    }

    @Test
    void shouldBindRoleWithoutSchool() {
        UserCommandPreferenceService preferences = mock(UserCommandPreferenceService.class);
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        when(preferences.bind(message, "乾坤一掷", "加菲", null))
                .thenReturn(userInfo("乾坤一掷", "加菲", null));

        BotResponse response = action(preferences).doRequest(
                message, "绑定角色 乾坤一掷 加菲", REGEX.BindRole);

        assertEquals("默认角色已绑定：乾坤一掷 加菲（门派未设置）", response.getContent());
        verify(preferences).bind(message, "乾坤一掷", "加菲", null);
    }
    @Test
    void shouldBindAndUnbindDefaultSchool() {
        UserCommandPreferenceService preferences = mock(UserCommandPreferenceService.class);
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        UserInfo userInfo = userInfo(null, null);
        userInfo.setSchool("万花");
        when(preferences.bindSchool(message, "万花")).thenReturn(userInfo);
        when(preferences.unbindSchool(message)).thenReturn(true);

        BotResponse bound = action(preferences).doRequest(message, "绑定门派 万花", REGEX.BindSchool);
        BotResponse unbound = action(preferences).doRequest(message, "解绑门派", REGEX.UnbindSchool);

        assertEquals("默认门派已绑定：万花", bound.getContent());
        assertEquals("默认门派已解除。", unbound.getContent());
    }

    @Test
    void shouldShowAllRolesAndMarkDefault() {
        UserCommandPreferenceService preferences = mock(UserCommandPreferenceService.class);
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        UserInfo selected = userInfo("乾坤一掷", "加菲");
        selected.setSchool("万花");
        when(preferences.findBindings(message)).thenReturn(new UserCommandPreferenceService.BindingSnapshot(
                selected, List.of(role("乾坤一掷", "加菲", "万花"), role("梦江南", "乔峰", "丐帮"))));

        BotResponse response = action(preferences).doRequest(message, "我的角色", REGEX.ShowRoleBinding);

        assertEquals("我的绑定：\n【默认】乾坤一掷 加菲 万花\n- 梦江南 乔峰 丐帮", response.getContent());
    }

    @Test
    void shouldDeleteOneRoleWithServerAndRoleName() {
        UserCommandPreferenceService preferences = mock(UserCommandPreferenceService.class);
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        when(preferences.deleteRole(message, "梦江南", "乔峰")).thenReturn(true);

        BotResponse one = action(preferences).doRequest(message, "删除角色 梦江南 乔峰", REGEX.UnbindRole);

        assertEquals("角色绑定已解除：梦江南 乔峰", one.getContent());
        verify(preferences).deleteRole(message, "梦江南", "乔峰");
    }

    private static UserBindingAction action(UserCommandPreferenceService preferences) {
        return new UserBindingAction(new ApiProperties(), mock(Jx3RequestUtil.class),
                mock(GroupConfigurationService.class), preferences);
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

    private static UserRoleBinding role(String server, String roleName) {
        return role(server, roleName, null);
    }

    private static UserRoleBinding role(String server, String roleName, String school) {
        UserRoleBinding role = new UserRoleBinding();
        role.setServer(server);
        role.setRoleName(roleName);
        role.setSchool(school);
        return role;
    }
}
