package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.data.server.sand.CastleInfo;
import com.grafie.botjava.jx3.http.data.server.sand.ServerSandData;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;

@Jx3Action

/**
 * 沙盘信息
 * @author grafie.chen
 * @since 2025/1/22  17:23
 */
public class ServerSandAction extends Jx3BaseAction {
    public ServerSandAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupConfigurationService groupConfigurationService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        Map<String, Object> params = new HashMap<>();
        params.put("server", currentArguments().server(getDefaultServer()));
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
        return "阵营沙盘";
    }

    @Override
    protected Map<String, Object> buildTemplateData(BaseResult baseResult) {
        ServerSandData result = (ServerSandData) baseResult.getData();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("zone", result.getZone());
        data.put("server", firstNotBlank(result.getServer(), currentArguments().server(getDefaultServer())));
        data.put("update", result.getUpdate());
        data.put("data", toViews(result.getData()));
        return data;
    }

    private List<CastleView> toViews(List<CastleInfo> values) {
        if (values == null) {
            return List.of();
        }
        List<CastleView> views = new ArrayList<>();
        for (CastleInfo item : values) {
            if (item == null) {
                continue;
            }
            views.add(new CastleView(item.getCastleName(), item.getTongName(),
                    item.getMasterName(), item.getCampName()));
        }
        return List.copyOf(views);
    }

    public record CastleView(String castleName, String tongName,
                             String masterName, String campName) {
    }
}
