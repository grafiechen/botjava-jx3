package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.data.tieba.TiebaItemRecordsData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;

@Jx3Action

/**
 * 贴吧物价
 * @author grafie.chen
 * @since 2025/1/22  17:24
 */
public class TiebaItemRecordsAction extends Jx3BaseAction {
    public TiebaItemRecordsAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupConfigurationService groupConfigurationService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        return Map.of(
                "server", currentArguments().server(getDefaultServer()),
                "name", currentArguments().roleName(),
                "limit", 10
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
        return "贴吧物价";
    }

    @Override
    protected Map<String, Object> buildTemplateData(BaseResult baseResult) {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("server", currentArguments().server(getDefaultServer()));
        template.put("name", currentArguments().roleName());
        template.put("data", toViews(baseResult == null ? null : baseResult.getData()));
        return template;
    }

    private List<RecordView> toViews(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        List<RecordView> views = new ArrayList<>();
        for (Object item : values) {
            if (item instanceof TiebaItemRecordsData record) {
                views.add(new RecordView(record.getZone(), record.getServer(), record.getName(),
                        record.getContext(), record.getReply(), record.getFloor(), record.getTime()));
            }
        }
        return List.copyOf(views);
    }

    public record RecordView(String zone, String server, String name, String context,
                             Long reply, Integer floor, String time) {
    }
}
