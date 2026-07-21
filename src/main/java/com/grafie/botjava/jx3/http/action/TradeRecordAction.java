package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.BaseResult;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;

@Jx3Action

/**
 * 物品价格
 * @author grafie.chen
 * @since 2025/1/22  17:24
 */
public class TradeRecordAction extends Jx3BaseAction {
    public TradeRecordAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupConfigurationService groupConfigurationService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        Map<String, Object> requestMap = new HashMap<>();
        String server = currentArguments().server(getDefaultServer());
        if (StringUtils.isNotBlank(server)) {
            requestMap.put("server", server);
        }
        requestMap.put("name", currentArguments().value());
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
        return "物品价格";
    }

    @Override
    protected Object buildTemplateData(BaseResult baseResult) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("mode", "物品价格");
        data.put("server", currentArguments().server(getDefaultServer()));
        data.put("name", currentArguments().value());
        data.put("data", baseResult.getData());
        return data;
    }
}
