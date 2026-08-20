package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.data.active.ActiveCurrentData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author grafie.chen
 * @since 2025/1/24  14:14
 */
@Jx3Action
public class ActiveCurrentAction extends Jx3BaseAction {

    public ActiveCurrentAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupConfigurationService groupConfigurationService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("server", currentArguments().server(getDefaultServer()));
        currentArguments().integer("value", 0, 30).ifPresent(num -> requestMap.put("num", num));
        return requestMap;
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
        return "活动日历";
    }

    @Override
    protected Map<String, Object> buildTemplateData(BaseResult baseResult) {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("server", currentArguments().server(getDefaultServer()));
        if (baseResult != null && baseResult.getData() instanceof ActiveCurrentData current) {
            template.put("data", new DailyView(current.getDate(), current.getWeek(), current.getWar(),
                    current.getBattle(), current.getOrecar(), current.getSchool(), current.getRescue(),
                    readable(current.getDraw()), readable(current.getLeader()), readable(current.getLuck()),
                    readable(current.getCard()), weeklyTasks(current)));
        }
        return template;
    }

    private List<String> readable(List<String> values) {
        if (values == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                result.add(value.trim());
            }
        }
        return List.copyOf(result);
    }

    private List<String> weeklyTasks(ActiveCurrentData current) {
        List<String> result = new ArrayList<>(readable(current.getTeam()));
        ActiveCurrentData.Weekly weekly = current.getWeekly();
        if (weekly != null) {
            addPrefixed(result, "公共任务", weekly.getConn());
            addPrefixed(result, "团队秘境", weekly.getRaid());
        }
        return List.copyOf(result);
    }

    private void addPrefixed(List<String> target, String prefix, List<String> values) {
        for (String value : readable(values)) {
            target.add(prefix + "：" + value);
        }
    }

    public record DailyView(String date, String week, String war, String battle, String orecar,
                            String school, String rescue, List<String> draws, List<String> leaders,
                            List<String> luck, List<String> cards, List<String> teams) {
    }
}
