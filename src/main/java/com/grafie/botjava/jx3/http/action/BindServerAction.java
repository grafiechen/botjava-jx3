package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;

import java.util.Map;

/**
 * @author grafie.chen
 * @since 2025/1/23  16:59
 */
@Jx3Action
public class BindServerAction extends Jx3BaseAction {
    private final GroupConfigurationService groupConfigurationService;

    public BindServerAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil,
                            GroupConfigurationService groupConfigurationService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
        this.groupConfigurationService = groupConfigurationService;
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        return null;
    }

    @Override
    protected BotResponse dealAfterJx3ApiRequest(BaseResult baseResult) {
        GroupConfigurationService.BindServerResult result = groupConfigurationService.bindServer(
                currentMessage().getGroupOpenid(), currentArguments().get("server"));
        return BotResponse.text(result.bound()
                ? String.format("默认服务器设置成功，[%s]", result.server())
                : String.format("默认服务器已存在，无法重复设置，当前设置为[%s]", result.server()));
    }
}
