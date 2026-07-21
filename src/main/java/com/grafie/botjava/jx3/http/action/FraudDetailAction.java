package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.data.fraud.detail.DetailInfo;
import com.grafie.botjava.jx3.http.data.fraud.detail.FraudDetailData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;


/**
 * 骗子记录
 * @author grafie.chen
 * @since 2025/1/22  17:31
 */
@Jx3Action
public class FraudDetailAction extends Jx3BaseAction {
    public FraudDetailAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupConfigurationService groupConfigurationService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        long uid = currentArguments().longInteger("uid", 10_000L, 9_999_999_999L).orElseThrow();
        return Map.of("uid", uid);
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
        return "骗子查询";
    }

    @Override
    protected Map<String, Object> buildTemplateData(BaseResult baseResult) {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("uid", currentArguments().get("uid"));
        template.put("data", toViews(baseResult == null ? null : baseResult.getData()));
        return template;
    }

    private List<FraudView> toViews(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        List<FraudView> views = new ArrayList<>();
        for (Object item : values) {
            if (item instanceof FraudDetailData group && group.getData() != null) {
                for (DetailInfo detail : group.getData()) {
                    if (detail != null) {
                        views.add(new FraudView(group.getServer(), group.getTieba(), detail.getTitle(),
                                detail.getText(), detail.getTime()));
                    }
                }
            }
        }
        return List.copyOf(views);
    }

    public record FraudView(String server, String tieba, String title, String text, String time) {
    }
}
