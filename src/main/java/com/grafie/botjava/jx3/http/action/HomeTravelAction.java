package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.data.home.HomeTravelData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;


/**
*
* 器物图谱
 * @author grafie.chen
* @since 2025/1/22  17:17
*/
@Jx3Action
public class HomeTravelAction extends Jx3BaseAction {
    public HomeTravelAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupConfigurationService groupConfigurationService) {
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
        return "器物图谱";
    }

    @Override
    protected Map<String, Object> buildTemplateData(BaseResult baseResult) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", currentArguments().get("name"));
        data.put("data", toViews(baseResult == null ? null : baseResult.getData()));
        return data;
    }

    private List<TravelView> toViews(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        List<TravelView> views = new ArrayList<>();
        for (Object item : values) {
            if (item instanceof HomeTravelData travel) {
                views.add(new TravelView(travel.getName(), travel.getSource(), travel.getLimit(), travel.getQuality(),
                        travel.getView(), travel.getPractical(), travel.getHard(), travel.getGeomantic(),
                        travel.getInteresting(), travel.getProduce(), travel.getTip()));
            }
        }
        return List.copyOf(views);
    }

    public record TravelView(String name, String source, int limit, int quality, int view,
                             int practical, int hard, int geomantic, int interesting,
                             String produce, String tip) {
    }
}
