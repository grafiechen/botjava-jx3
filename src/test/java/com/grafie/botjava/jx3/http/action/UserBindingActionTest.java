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
        when(preferences.bind(message, "乾坤一掷", "加菲")).thenReturn(userInfo("乾坤一掷", "加菲"));

        BotResponse response = action(preferences).doRequest(
                message, "绑定角色 乾坤一掷 加菲", REGEX.BindRole);

        assertEquals("角色绑定成功：乾坤一掷 加菲", response.getContent());
        verify(preferences).bind(message, "乾坤一掷", "加菲");
    }

    @Test
    void shouldAddAndSwitchPersonalRole() {
        UserCommandPreferenceService preferences = mock(UserCommandPreferenceService.class);
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        when(preferences.addRole(message, "梦江南", "乔峰"))
                .thenReturn(role("梦江南", "乔峰"));
        when(preferences.switchRole(message, "梦江南", "乔峰"))
                .thenReturn(userInfo("梦江南", "乔峰"));

        BotResponse added = action(preferences).doRequest(message, "添加角色 梦江南 乔峰", REGEX.AddRole);
        BotResponse switched = action(preferences).doRequest(message, "切换角色 梦江南 乔峰", REGEX.SwitchRole);

        assertEquals("常用角色已添加：梦江南 乔峰", added.getContent());
        assertEquals("默认角色已切换：梦江南 乔峰", switched.getContent());
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
                selected, List.of(role("乾坤一掷", "加菲"), role("梦江南", "乔峰"))));

        BotResponse response = action(preferences).doRequest(message, "我的角色", REGEX.ShowRoleBinding);

        assertEquals("我的绑定：\n默认门派：万花\n【默认】乾坤一掷 加菲\n- 梦江南 乔峰", response.getContent());
    }

    @Test
    void shouldUnbindOneRoleOrAllRoles() {
        UserCommandPreferenceService preferences = mock(UserCommandPreferenceService.class);
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        when(preferences.unbindRole(message, "梦江南", "乔峰")).thenReturn(true);
        when(preferences.unbind(message)).thenReturn(true);

        BotResponse one = action(preferences).doRequest(message, "解绑角色 梦江南 乔峰", REGEX.UnbindRole);
        BotResponse all = action(preferences).doRequest(message, "解绑角色", REGEX.UnbindRole);

        assertEquals("角色绑定已解除：梦江南 乔峰", one.getContent());
        assertEquals("角色绑定已解除。", all.getContent());
        verify(preferences).unbindRole(message, "梦江南", "乔峰");
        verify(preferences).unbind(message);
    }

    private static UserBindingAction action(UserCommandPreferenceService preferences) {
        return new UserBindingAction(new ApiProperties(), mock(Jx3RequestUtil.class),
                mock(GroupConfigurationService.class), preferences);
    }

    private static UserInfo userInfo(String server, String roleName) {
        UserInfo userInfo = new UserInfo();
        userInfo.setServer(server);
        userInfo.setRoleName(roleName);
        return userInfo;
    }

    private static UserRoleBinding role(String server, String roleName) {
        UserRoleBinding role = new UserRoleBinding();
        role.setServer(server);
        role.setRoleName(roleName);
        return role;
    }
}
