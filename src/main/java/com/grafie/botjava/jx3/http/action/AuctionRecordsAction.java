package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.data.acution.AcutionRecordsData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;


/**
 * 拍卖记录
 * 阵营拍卖记录。
 *  @author grafie.chen
 * @since 2025/1/22  17:28
 */
@Jx3Action
public class AuctionRecordsAction extends Jx3BaseAction {
    public AuctionRecordsAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupConfigurationService groupConfigurationService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        java.util.HashMap<String, Object> params = new java.util.HashMap<>();
        params.put("server", currentArguments().server(getDefaultServer()));
        if (currentArguments().get("name") != null) {
            params.put("name", currentArguments().get("name"));
        }
        params.put("limit", 20);
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
        return "阵营拍卖";
    }

    @Override
    protected Map<String, Object> buildTemplateData(BaseResult baseResult) {
        List<AuctionRecordView> views = toViews(baseResult == null ? null : baseResult.getData());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("server", views.isEmpty() ? getDefaultServer() : views.get(0).server());
        data.put("name", currentArguments().get("name"));
        data.put("data", views);
        return data;
    }

    private List<AuctionRecordView> toViews(Object data) {
        if (!(data instanceof List<?> records)) {
            return List.of();
        }
        List<AuctionRecordView> views = new ArrayList<>();
        for (Object value : records) {
            if (!(value instanceof AcutionRecordsData record)) {
                continue;
            }
            views.add(new AuctionRecordView(
                    record.getZone(), record.getServer(), record.getMapName(), record.getRoleName(),
                    record.getCampName(), record.getItemName(), record.getItemAmount(), record.getTime()
            ));
        }
        return List.copyOf(views);
    }

    public record AuctionRecordView(String zone, String server, String mapName, String roleName,
                                    String campName, String itemName, String itemAmount, String time) {
    }
}
