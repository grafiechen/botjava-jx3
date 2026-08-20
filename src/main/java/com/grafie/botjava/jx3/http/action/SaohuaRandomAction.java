package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.data.saohua.SaohuaRandomData;

import java.util.Map;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;


/**
 * 撩人骚话
 *
 * @author grafie.chen
 * @since 2025/1/22  17:19
 */
@Jx3Action
public class SaohuaRandomAction extends Jx3BaseAction {
    public SaohuaRandomAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupConfigurationService groupConfigurationService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        return Map.of();
    }

    @Override
    protected BotResponse dealAfterJx3ApiRequest(BaseResult baseResult) {
        SaohuaRandomData data = (SaohuaRandomData) baseResult.getData();
        if (data.getText() == null || data.getText().isBlank()) {
            return BotResponse.text("暂无骚话。");
        }
        return BotResponse.text(data.getText().trim());
    }
}
