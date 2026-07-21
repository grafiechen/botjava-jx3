package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.data.active.monster.ActiveMonsterData;
import com.grafie.botjava.jx3.http.data.active.monster.MonsterInfo;
import com.grafie.botjava.jx3.http.data.active.monster.OtherData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;


/**
 * 百战首领
 * @author grafie.chen
 * @since 2025/1/22  17:24
 */
@Jx3Action
public class ActiveMonsterAction extends Jx3BaseAction {
    public ActiveMonsterAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupConfigurationService groupConfigurationService) {
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
        return "百战首领";
    }

    @Override
    protected Map<String, Object> buildTemplateData(BaseResult baseResult) {
        ActiveMonsterData monster = baseResult != null && baseResult.getData() instanceof ActiveMonsterData value
                ? value : null;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("server", currentArguments().server(getDefaultServer()));
        data.put("week", monster == null ? null : monster.getWeek());
        data.put("boss", monster == null ? null : monster.getBoss());
        data.put("start", monster == null ? null : monster.getStart());
        data.put("end", monster == null ? null : monster.getEnd());
        data.put("data", toViews(monster == null ? null : monster.getList()));
        return data;
    }

    private List<MonsterView> toViews(List<MonsterInfo> monsters) {
        if (monsters == null || monsters.isEmpty()) {
            return List.of();
        }
        List<MonsterView> views = new ArrayList<>();
        for (MonsterInfo monster : monsters) {
            if (monster == null) {
                continue;
            }
            OtherData extra = monster.getData();
            views.add(new MonsterView(
                    monster.getIndex(), monster.getName(), safeList(monster.getSkill()),
                    extra == null ? null : extra.getName(),
                    extra == null ? List.of() : safeList(extra.getList()),
                    extra == null ? null : extra.getDescription()
            ));
        }
        return List.copyOf(views);
    }

    private List<String> safeList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream().filter(value -> value != null && !value.isBlank()).toList();
    }

    public record MonsterView(Integer index, String name, List<String> skills,
                              String extraName, List<String> effects, String description) {
    }
}
