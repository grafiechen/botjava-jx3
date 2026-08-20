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
public class TuilanAchievementAction extends OfficialExtensionActionSupport {
    public TuilanAchievementAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil,
                     GroupConfigurationService groupConfigurationService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("server", currentArguments().get("server"));
        params.put("name", currentArguments().get("roleName"));
        params.put("class", currentArguments().integer("category", 1, 3).orElse(1));
        String subclass = currentArguments().get("subclass");
        if (subclass != null) {
            params.put("subclass", subclass);
        }
        if (apiProperties.getTicket() != null && !apiProperties.getTicket().isBlank()) {
            params.put("ticket", apiProperties.getTicket());
        }
        return params;
    }

    @Override
    protected BotResponse dealAfterJx3ApiRequest(BaseResult baseResult) {
        return buildReadableText("资历分布", baseResult);
    }
}