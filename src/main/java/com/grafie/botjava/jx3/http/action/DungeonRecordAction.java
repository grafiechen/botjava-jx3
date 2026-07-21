package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.BaseResult;

import java.util.Map;
import java.util.HashMap;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import com.grafie.botjava.jx3.http.data.role.teamcd.RoleTeamCdListData;

import java.util.LinkedHashMap;

/**
 * 副本记录
 *
 * @author grafie.chen
 * @since 2025/1/23  16:30
 */
@Jx3Action
public class DungeonRecordAction extends Jx3BaseAction {
    public DungeonRecordAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupConfigurationService groupConfigurationService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        Map<String, Object> params = new HashMap<>();
        params.put("server", currentArguments().server(getDefaultServer()));
        params.put("name", currentArguments().roleName());
        params.put("ticket", apiProperties.getTicket());
        return params;
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
        return "副本进度";
    }

    @Override
    protected Object buildTemplateData(BaseResult baseResult) {
        RoleTeamCdListData result = (RoleTeamCdListData) baseResult.getData();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("server", firstNotBlank(result.getServerName(), currentArguments().server(getDefaultServer())));
        data.put("name", firstNotBlank(result.getRoleName(), currentArguments().roleName()));
        data.put("globalId", result.getGlobalRoleId());
        data.put("data", result.getData());
        return data;
    }
}
