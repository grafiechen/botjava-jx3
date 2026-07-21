package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.data.home.HomeFurnitureData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;


/**
 * 家园装饰
 * @author grafie.chen
 * @since 2025/1/22  17:16
 */
@Jx3Action
public class HomeFurnitureAction extends Jx3BaseAction {
    public HomeFurnitureAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupConfigurationService groupConfigurationService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        return Map.of("name", currentArguments().get("name"));
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
        return "家园装饰";
    }

    @Override
    protected Map<String, Object> buildTemplateData(BaseResult baseResult) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", currentArguments().get("name"));
        data.put("data", toViews(baseResult == null ? null : baseResult.getData()));
        return data;
    }

    private List<FurnitureView> toViews(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        List<FurnitureView> views = new ArrayList<>();
        for (Object item : values) {
            if (item instanceof HomeFurnitureData furniture) {
                views.add(new FurnitureView(furniture.getName(), furniture.getSource(), furniture.getLimit(),
                        furniture.getQuality(), furniture.getView(), furniture.getPractical(), furniture.getHard(),
                        furniture.getGeomantic(), furniture.getInteresting(), furniture.getProduce(), furniture.getTip()));
            }
        }
        return List.copyOf(views);
    }

    public record FurnitureView(String name, String source, int limit, int quality, int view,
                                int practical, int hard, int geomantic, int interesting,
                                String produce, String tip) {
    }
}
