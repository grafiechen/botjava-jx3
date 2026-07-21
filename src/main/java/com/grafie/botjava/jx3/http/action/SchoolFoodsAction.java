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
public class SchoolFoodsAction extends Jx3BaseAction {
    public SchoolFoodsAction(ApiProperties apiProperties, Jx3RequestUtil requestUtil, GroupConfigurationService groupConfigurationService) {
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
        return "小药推荐";
    }

    @Override
    protected Map<String, Object> buildTemplateData(BaseResult baseResult) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("data", toViews(baseResult == null ? null : baseResult.getData()));
        return data;
    }

    private List<FoodView> toViews(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        List<FoodView> views = new ArrayList<>();
        for (Object item : values) {
            if (item instanceof OfficialQueryData.SchoolFood food) {
                views.add(new FoodView(food.getSchool(), food.getKungfu(), food.getColor(),
                        food.getCategory(), food.getName(), food.getBoost()));
            }
        }
        return List.copyOf(views);
    }

    public record FoodView(String school, String kungfu, String color,
                           String category, String name, String boost) {
    }
}
