package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.data.horse.HorseRecordsData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;


/**
 *的卢统计
 *  @author grafie.chen
 * @since 2025/1/22  17:29
 */
@Jx3Action
public class HorseRecordsAction extends Jx3BaseAction {
    public HorseRecordsAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupConfigurationService groupConfigurationService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        return Map.of("server", currentArguments().server(getDefaultServer()));
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
        return "的卢记录";
    }

    @Override
    protected Map<String, Object> buildTemplateData(BaseResult baseResult) {
        List<SteedRecordView> views = toViews(baseResult == null ? null : baseResult.getData());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("server", views.isEmpty() ? getDefaultServer() : views.get(0).server());
        data.put("data", views);
        return data;
    }

    private List<SteedRecordView> toViews(Object data) {
        if (!(data instanceof List<?> records)) {
            return List.of();
        }
        List<SteedRecordView> views = new ArrayList<>();
        for (Object value : records) {
            if (!(value instanceof HorseRecordsData record)) {
                continue;
            }
            views.add(new SteedRecordView(
                    record.getZone(), record.getServer(), record.getMapName(),
                    record.getRefreshTime(), record.getCaptureRoleName(), record.getCaptureCampName(),
                    record.getCaptureTime(), record.getAuctionRoleName(), record.getAuctionCampName(),
                    record.getAuctionTime(), record.getAuctionAmount(), record.getStartTime(), record.getEndTime()
            ));
        }
        return List.copyOf(views);
    }

    public record SteedRecordView(String zone, String server, String mapName, String refreshTime,
                                  String captureRoleName, String captureCampName, String captureTime,
                                  String auctionRoleName, String auctionCampName, String auctionTime,
                                  String auctionAmount, String startTime, String endTime) {
    }
}
