package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import com.grafie.botjava.service.SystemStatusService;

import java.util.Map;

@Jx3Action
public class SystemStatusAction extends Jx3BaseAction {

    private final SystemStatusService systemStatusService;

    public SystemStatusAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil,
                              GroupConfigurationService groupConfigurationService,
                              SystemStatusService systemStatusService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
        this.systemStatusService = systemStatusService;
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        return Map.of();
    }

    @Override
    protected BotResponse dealAfterJx3ApiRequest(BaseResult baseResult) {
        return BotResponse.text(systemStatusService.buildStatusText());
    }
}
