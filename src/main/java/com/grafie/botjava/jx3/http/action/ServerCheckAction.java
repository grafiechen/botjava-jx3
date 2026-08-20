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
import com.grafie.botjava.util.TimeUtils;
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
        String server = firstNotBlank(serverCheckData.getServer(), currentArguments().server(getDefaultServer()));
        String status = readableStatus(serverCheckData.getStatus());
        String time = firstNotBlank(serverCheckData.getTime(), TimeUtils.timeFormatting(baseResult.getTime() == null ? 0L : baseResult.getTime()));
        if (time == null) {
            return String.format("服务器[%s]：%s", server, status);
        }
        return String.format("服务器[%s]：%s，开服时间：%s", server, status, time);
    }

    private String readableStatus(String status) {
        if (status == null || status.isBlank()) {
            return "状态未知";
        }
        return switch (status.trim()) {
            case "1", "true", "TRUE", "已开服", "开服" -> "已开服";
            case "0", "false", "FALSE", "未开服", "维护", "维护中" -> "维护中";
            default -> status.trim();
        };
    }
}
