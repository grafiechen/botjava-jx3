package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.data.server.ServerCheckData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import com.grafie.botjava.entity.dto.common.BotResponse;
import java.util.HashMap;
import java.util.Map;

@Jx3Action

/**
 * 开服检查
 *
 * @author grafie.chen
 * @since 2025/1/22  17:18
 */
public class ServerCheckAction extends Jx3BaseAction {
    public ServerCheckAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupConfigurationService groupConfigurationService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("server", currentArguments().server(getDefaultServer()));
        requestMap.put("type", 1);
        return requestMap;
    }

    @Override
    protected BotResponse dealAfterJx3ApiRequest(BaseResult baseResult) {
        return buildMessageByTemplate(baseResult);
    }

    @Override
    protected String buildTextContent(BaseResult baseResult) {
        ServerCheckData serverCheckData = (ServerCheckData) baseResult.getData();
        return String.format(
                "服务器[%s],%s",
                serverCheckData.getServer(),
                serverCheckData.getStatus()
        );
    }
}
