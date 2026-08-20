package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.action.base.OfficialExtensionActionSupport;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;

import java.util.LinkedHashMap;
import java.util.Map;

@Jx3Action
public class SchoolSearchAction extends OfficialExtensionActionSupport {
    public SchoolSearchAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil,
                     GroupConfigurationService groupConfigurationService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", currentArguments().get("name"));
        String mode = currentArguments().get("mode");
        if (mode != null) {
            params.put("mode", mode);
        }
        params.put("ticket", apiProperties.getTicket());
        return params;
    }

    @Override
    protected BotResponse dealAfterJx3ApiRequest(BaseResult baseResult) {
        return buildReadableText("配装搜索", baseResult);
    }
}