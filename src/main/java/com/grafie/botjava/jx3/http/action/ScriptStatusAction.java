package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import com.grafie.botjava.service.ScriptStatusService;

import java.util.Map;

@Jx3Action
public class ScriptStatusAction extends Jx3BaseAction {

    private final ScriptStatusService scriptStatusService;

    public ScriptStatusAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil,
                              GroupConfigurationService groupConfigurationService,
                              ScriptStatusService scriptStatusService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
        this.scriptStatusService = scriptStatusService;
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        return Map.of();
    }

    @Override
    protected BotResponse dealAfterJx3ApiRequest(BaseResult baseResult) {
        String server = currentArguments().server(null);
        String roleName = currentArguments().roleName();
        return switch (currentRegex()) {
            case ScriptStatus -> scriptStatusService.query(currentMessage(), server, roleName);
            case RoleFieldQuery -> scriptStatusService.queryField(
                    currentMessage(), server, roleName, currentArguments().get("name"));
            case AllRoleFieldQuery -> scriptStatusService.queryAllFields(currentArguments().get("name"));
            case BagSpaceWarning -> scriptStatusService.queryBagSpaceWarning();
            case ScriptStatusUpdate -> scriptStatusService.update(
                    currentMessage(), server, roleName,
                    currentArguments().get("name"), currentArguments().get("text"));
            default -> BotResponse.text("不支持的脚本状态操作。");
        };
    }
}
