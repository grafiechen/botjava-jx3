package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.data.horse.HorseRanchData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;


/**
 * 马场事件
 * @author grafie.chen
 * @since 2025/1/22  17:29
 */
@Jx3Action
public class HorseEventAction extends Jx3BaseAction{
    public HorseEventAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupConfigurationService groupConfigurationService) {
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
        return "马场刷新";
    }

    @Override
    protected Map<String, Object> buildTemplateData(BaseResult baseResult) {
        HorseRanchData ranch = baseResult != null && baseResult.getData() instanceof HorseRanchData value
                ? value : null;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("zone", ranch == null ? null : ranch.getZone());
        data.put("server", ranch == null ? getDefaultServer() : firstNotBlank(ranch.getServer(), getDefaultServer()));
        data.put("note", ranch == null ? null : ranch.getNote());
        data.put("data", toViews(ranch == null ? null : ranch.getData()));
        return data;
    }

    private List<RanchPredictionView> toViews(Map<String, List<String>> predictions) {
        if (predictions == null || predictions.isEmpty()) {
            return List.of();
        }
        List<RanchPredictionView> views = new ArrayList<>();
        predictions.forEach((mapName, values) -> views.add(new RanchPredictionView(
                mapName,
                values == null ? List.of() : values.stream()
                        .filter(value -> value != null && !value.isBlank())
                        .toList()
        )));
        return List.copyOf(views);
    }

    public record RanchPredictionView(String mapName, List<String> predictions) {
    }
}
