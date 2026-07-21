package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.data.official.OfficialQueryData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Jx3Action
public class MineCartAction extends Jx3BaseAction {
    public MineCartAction(ApiProperties apiProperties, Jx3RequestUtil requestUtil,
                          GroupConfigurationService groupConfigurationService) {
        super(apiProperties, requestUtil, groupConfigurationService);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        return Map.of();
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
        return "关隘首领";
    }

    @Override
    protected Map<String, Object> buildTemplateData(BaseResult baseResult) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("server", "全服");
        data.put("data", toViews(baseResult == null ? null : baseResult.getData()));
        return data;
    }

    private List<MineCartView> toViews(Object data) {
        if (!(data instanceof List<?> groups)) {
            return List.of();
        }
        List<MineCartView> views = new ArrayList<>();
        for (Object groupValue : groups) {
            if (!(groupValue instanceof OfficialQueryData.MineCart group) || group.getData() == null) {
                continue;
            }
            for (OfficialQueryData.MineCartRecord record : group.getData()) {
                if (record == null) {
                    continue;
                }
                views.add(new MineCartView(
                        record.getZone(),
                        firstNotBlank(record.getServer(), group.getServer()),
                        record.getLeader(),
                        record.getCampName(),
                        record.getCastle(),
                        record.getStatusText()
                ));
            }
        }
        return List.copyOf(views);
    }

    public record MineCartView(String zone, String server, String leader,
                               String campName, String castle, String statusText) {
    }
}
