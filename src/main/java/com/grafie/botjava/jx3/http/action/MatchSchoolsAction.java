package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.data.match.MatchSchoolsData;

import java.util.LinkedHashMap;
import java.util.Map;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;


/**
 * 名剑统计
 * @author grafie.chen
 * @since 2025/1/22  17:21
 */
@Jx3Action
public class MatchSchoolsAction extends Jx3BaseAction {
    public MatchSchoolsAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupConfigurationService groupConfigurationService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        return Map.of(
                "mode", currentArguments().integerOneOf("mode", 22, 33, 55).orElse(33),
                "ticket", apiProperties.getTicket()
        );
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
        return "名剑统计";
    }

    @Override
    protected Object buildTemplateData(BaseResult baseResult) {
        Object responseData = baseResult == null ? null : baseResult.getData();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("mode", currentArguments().integerOneOf("mode", 22, 33, 55).orElse(33));
        data.put("maxValue", resolveMaxValue(responseData));
        data.put("data", responseData);
        return data;
    }

    private double resolveMaxValue(Object responseData) {
        double maxValue = 1D;
        if (!(responseData instanceof Iterable<?> rows)) {
            return maxValue;
        }
        for (Object row : rows) {
            if (!(row instanceof MatchSchoolsData school)) {
                continue;
            }
            maxValue = Math.max(maxValue, absoluteValue(school.getLast()));
            maxValue = Math.max(maxValue, absoluteValue(school.getCurrent()));
        }
        return maxValue;
    }

    private double absoluteValue(Integer value) {
        return value == null ? 0D : Math.abs(value.doubleValue());
    }
}
