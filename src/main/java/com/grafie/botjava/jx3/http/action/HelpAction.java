package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import com.grafie.botjava.service.HelpMenuService;

import java.util.Map;

/**
 * 根据统一指令注册定义生成帮助菜单。
 */
@Jx3Action
public class HelpAction extends Jx3BaseAction {

    private final HelpMenuService helpMenuService;

    public HelpAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil,
                      GroupConfigurationService groupConfigurationService, HelpMenuService helpMenuService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
        this.helpMenuService = helpMenuService;
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        return Map.of();
    }

    @Override
    protected BotResponse dealAfterJx3ApiRequest(BaseResult baseResult) {
        if ("菜单".equals(REGEX.normalizeCommand(currentRequestRegex()))) {
            return helpMenuService.interactiveMenu();
        }
        return helpMenuService.textHelp(currentMessage().getGroupOpenid());
    }
}
