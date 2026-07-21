package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import java.util.HashMap;
import java.util.Map;


/**
 * 装备属性
 *
 * @author grafie.chen
 * @since 2025/1/22  17:22
 */
@Jx3Action
public class RoleAttributeAction extends Jx3BaseAction {
    public RoleAttributeAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupConfigurationService groupConfigurationService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("server", currentArguments().server(getDefaultServer()));
        requestMap.put("name", currentArguments().roleName());
        requestMap.put("ticket", apiProperties.getTicket());
        return requestMap;
    }

    @Override
    protected BotResponse dealAfterJx3ApiRequest(BaseResult baseResult) {
        return buildMessageByTemplate(baseResult);
    }

    @Override
    protected BotResponse.ResponseType getResponseType() {
        return BotResponse.ResponseType.IMAGE;
    }

    @Override
    protected String getTemplatePath() {
        return "角色装备";
    }

    @Override
    protected Object buildTemplateData(BaseResult baseResult) {
        return buildStandardTemplateData(baseResult);
    }
}
