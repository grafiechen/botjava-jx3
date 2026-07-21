package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.UserInfo;
import com.grafie.botjava.entity.UserRoleBinding;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import com.grafie.botjava.service.UserCommandPreferenceService;

import java.util.Map;

@Jx3Action
public class UserBindingAction extends Jx3BaseAction {

    private final UserCommandPreferenceService preferenceService;

    public UserBindingAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil,
                             GroupConfigurationService groupConfigurationService, UserCommandPreferenceService preferenceService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
        this.preferenceService = preferenceService;
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        return Map.of();
    }

    @Override
    protected BotResponse dealAfterJx3ApiRequest(BaseResult baseResult) {
        return switch (currentRegex()) {
            case BindSchool -> bindSchool();
            case UnbindSchool -> unbindSchool();
            case BindRole -> bind();
            case AddRole -> add();
            case SwitchRole -> switchRole();
            case ShowRoleBinding -> show();
            case UnbindRole -> unbind();
            default -> BotResponse.text("不支持的用户绑定操作。");
        };
    }

    private BotResponse bindSchool() {
        UserInfo userInfo = preferenceService.bindSchool(currentMessage(), currentArguments().school());
        return BotResponse.text("默认门派已绑定：" + userInfo.getSchool());
    }

    private BotResponse unbindSchool() {
        return BotResponse.text(preferenceService.unbindSchool(currentMessage())
                ? "默认门派已解除。" : "当前没有默认门派。");
    }

    private BotResponse bind() {
        UserInfo userInfo = preferenceService.bind(
                currentMessage(), currentArguments().get("server"), currentArguments().roleName());
        return BotResponse.text("角色绑定成功：" + userInfo.getServer() + " " + userInfo.getRoleName());
    }

    private BotResponse add() {
        UserRoleBinding binding = preferenceService.addRole(
                currentMessage(), currentArguments().get("server"), currentArguments().roleName());
        return BotResponse.text("常用角色已添加：" + binding.getServer() + " " + binding.getRoleName());
    }

    private BotResponse switchRole() {
        UserInfo userInfo = preferenceService.switchRole(
                currentMessage(), currentArguments().get("server"), currentArguments().roleName());
        return BotResponse.text("默认角色已切换：" + userInfo.getServer() + " " + userInfo.getRoleName());
    }

    private BotResponse show() {
        UserCommandPreferenceService.BindingSnapshot snapshot = preferenceService.findBindings(currentMessage());
        String school = snapshot.defaultRole() == null ? null : snapshot.defaultRole().getSchool();
        if (snapshot.roles().isEmpty() && school == null) {
            return BotResponse.text("暂未设置个人绑定。");
        }
        StringBuilder content = new StringBuilder("我的绑定：");
        if (school != null) {
            content.append("\n默认门派：").append(school);
        }
        for (UserRoleBinding role : snapshot.roles()) {
            boolean selected = sameRole(role, snapshot.defaultRole());
            content.append("\n")
                    .append(selected ? "【默认】" : "- ")
                    .append(role.getServer()).append(" ").append(role.getRoleName());
        }
        return BotResponse.text(content.toString());
    }

    private BotResponse unbind() {
        String server = currentArguments().get("server");
        String roleName = currentArguments().roleName();
        if (server != null && roleName != null) {
            boolean removed = preferenceService.unbindRole(currentMessage(), server, roleName);
            return BotResponse.text(removed
                    ? "角色绑定已解除：" + server + " " + roleName
                    : "未找到该角色绑定。");
        }
        return BotResponse.text(preferenceService.unbind(currentMessage())
                ? "角色绑定已解除。" : "当前没有角色绑定。");
    }

    private boolean sameRole(UserRoleBinding role, UserInfo defaultRole) {
        return role != null && defaultRole != null
                && java.util.Objects.equals(role.getServer(), defaultRole.getServer())
                && java.util.Objects.equals(role.getRoleName(), defaultRole.getRoleName());
    }
}
