package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.data.active.calendar.ActiveCalendarData;
import com.grafie.botjava.jx3.http.data.active.calendar.DataInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;


/**
 * 活动月历
 * @author grafie.chen
 * @since 2025/1/22  17:15
 */
@Jx3Action
public class ActiveListCalendarAction extends Jx3BaseAction {
    public ActiveListCalendarAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupConfigurationService groupConfigurationService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        java.util.HashMap<String, Object> params = new java.util.HashMap<>();
        currentArguments().integer("num", 1, 31).ifPresent(num -> params.put("num", num));
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
        return "活动月历";
    }

    @Override
    protected Map<String, Object> buildTemplateData(BaseResult baseResult) {
        Map<String, Object> template = new LinkedHashMap<>();
        if (baseResult != null && baseResult.getData() instanceof ActiveCalendarData calendar) {
            if (calendar.getToday() != null) {
                template.put("today", new TodayView(calendar.getToday().getDate(), calendar.getToday().getWeek()));
            }
            template.put("data", toViews(calendar.getData()));
        }
        return template;
    }

    private List<CalendarDayView> toViews(List<DataInfo> values) {
        if (values == null) {
            return List.of();
        }
        List<CalendarDayView> views = new ArrayList<>();
        for (DataInfo value : values) {
            if (value != null) {
                views.add(new CalendarDayView(value.getDate(), value.getWeek(), value.getWar(), value.getBattle(),
                        value.getOrecar(), value.getSchool(), value.getRescue(), readable(value.getLuck()),
                        readable(value.getCard())));
            }
        }
        return List.copyOf(views);
    }

    private List<String> readable(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream().filter(value -> value != null && !value.isBlank()).map(String::trim).toList();
    }

    public record TodayView(String date, String week) {
    }

    public record CalendarDayView(String date, String week, String war, String battle, String orecar,
                                  String school, String rescue, List<String> luck, List<String> cards) {
    }
}
