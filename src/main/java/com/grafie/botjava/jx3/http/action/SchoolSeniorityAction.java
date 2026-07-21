package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 资历排行查询与图片数据拼装。
 */
@Jx3Action
public class SchoolSeniorityAction extends Jx3BaseAction {

    public SchoolSeniorityAction(ApiProperties apiProperties, Jx3RequestUtil requestUtil,
                                 GroupConfigurationService groupConfigurationService) {
        super(apiProperties, requestUtil, groupConfigurationService);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        Map<String, Object> params = new LinkedHashMap<>();
        putIfPresent(params, "server", currentArguments().get("server"));
        putIfPresent(params, "school", currentArguments().get("school"));
        putIfPresent(params, "ticket", apiProperties.getTicket());
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
        return "资历排行";
    }

    @Override
    protected Object buildTemplateData(BaseResult baseResult) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("server", currentArguments().get("server"));
        data.put("school", currentArguments().get("school"));
        data.put("data", baseResult == null ? null : baseResult.getData());
        return data;
    }

    private void putIfPresent(Map<String, Object> params, String key, Object value) {
        if (value != null) {
            params.put(key, value);
        }
    }
}
