package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.data.trade.TradeDemonData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;

@Jx3Action

/**
 * 金币比例
 * @author grafie.chen
 * @since 2025/1/22  17:19
 */
public class TradeDemonAction extends Jx3BaseAction {
    public TradeDemonAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupConfigurationService groupConfigurationService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        Map<String, Object> params = new HashMap<>();
        params.put("server", currentArguments().server(getDefaultServer()));
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
        return "金币价格";
    }

    @Override
    protected Map<String, Object> buildTemplateData(BaseResult baseResult) {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("server", currentArguments().server(getDefaultServer()));
        template.put("data", toViews(baseResult == null ? null : baseResult.getData()));
        return template;
    }

    private List<PriceView> toViews(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        List<PriceView> views = new ArrayList<>();
        for (Object item : values) {
            if (item instanceof TradeDemonData price) {
                views.add(new PriceView(price.getZone(), price.getServer(), price.getTieba(),
                        price.getWanbaolou(), price.getDd373(), price.getDate()));
            }
        }
        return List.copyOf(views);
    }

    public record PriceView(String zone, String server, String tieba,
                            String wanbaolou, String dd373, String date) {
    }
}
