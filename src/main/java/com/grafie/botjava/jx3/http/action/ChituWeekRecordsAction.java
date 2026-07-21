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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Jx3Action
public class ChituWeekRecordsAction extends Jx3BaseAction {
    public ChituWeekRecordsAction(ApiProperties apiProperties, Jx3RequestUtil requestUtil,
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
        return "赤兔记录";
    }

    @Override
    protected Map<String, Object> buildTemplateData(BaseResult baseResult) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("mode", "本周");
        data.put("server", "全服");
        data.put("data", toViews(baseResult == null ? null : baseResult.getData()));
        return data;
    }

    private List<ChituWeekRecordView> toViews(Object data) {
        if (!(data instanceof List<?> records)) {
            return List.of();
        }
        List<ChituWeekRecordView> views = new ArrayList<>();
        for (Object value : records) {
            if (value instanceof OfficialQueryData.ChituWeekRecord record) {
                views.add(new ChituWeekRecordView(
                        record.getServer(), record.getMapName(), record.getHorse(), record.getDate()));
            }
        }
        return List.copyOf(views);
    }

    public record ChituWeekRecordView(String server, String mapName, String horse, String date) {
    }
}
