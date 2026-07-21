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
 * 黑市物价查询与图片数据拼装。
 */
@Jx3Action
public class TradeRecordsAction extends Jx3BaseAction {

    public TradeRecordsAction(ApiProperties apiProperties, Jx3RequestUtil requestUtil,
                              GroupConfigurationService groupConfigurationService) {
        super(apiProperties, requestUtil, groupConfigurationService);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        return Map.of(
                "server", currentArguments().server(getDefaultServer()),
                "name", currentArguments().roleName()
        );
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
        return "物品价格";
    }

    @Override
    protected Object buildTemplateData(BaseResult baseResult) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("mode", "黑市物价");
        data.put("server", currentArguments().server(getDefaultServer()));
        data.put("name", currentArguments().roleName());
        data.put("data", baseResult == null ? null : baseResult.getData());
        return data;
    }
}
