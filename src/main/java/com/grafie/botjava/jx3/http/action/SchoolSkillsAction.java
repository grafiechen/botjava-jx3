package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.data.school.skill.SchoolSkillsData;
import com.grafie.botjava.jx3.http.data.school.skill.Skill;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;

@Jx3Action

/**
 * 技能效果
 * @author grafie.chen
 * @since 2025/1/22  17:23
 */
public class SchoolSkillsAction extends Jx3BaseAction {
    public SchoolSkillsAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupConfigurationService groupConfigurationService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        return Map.of(
                "name", currentArguments().get("name"),
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
        return "技能详情";
    }

    @Override
    protected Map<String, Object> buildTemplateData(BaseResult baseResult) {
        List<SchoolSkillsData> groups = extractGroups(baseResult == null ? null : baseResult.getData());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", currentArguments().get("name"));
        data.put("groups", groups.stream().filter(group -> group != null)
                .map(group -> new SkillGroupView(group.getCategory(), toViews(group.getSkills())))
                .toList());
        return data;
    }

    private List<SchoolSkillsData> extractGroups(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        List<SchoolSkillsData> groups = new ArrayList<>();
        for (Object item : values) {
            if (item instanceof SchoolSkillsData group) {
                groups.add(group);
            }
        }
        return List.copyOf(groups);
    }

    private List<SkillView> toViews(List<Skill> skills) {
        if (skills == null || skills.isEmpty()) {
            return List.of();
        }
        return skills.stream()
                .filter(skill -> skill != null && skill.getName() != null && !skill.getName().isBlank())
                .map(skill -> new SkillView(
                        skill.getName(), skill.getSimpleDescription(), skill.getDescription(),
                        skill.getSpecialDescription(), skill.getInterval(), skill.getConsumption(),
                        skill.getDistance(), skill.getKind(), skill.getSubKind(),
                        skill.getReleaseType(), skill.getWeapon()))
                .toList();
    }

    public record SkillGroupView(String category, List<SkillView> skills) {
    }

    public record SkillView(String name, String summary, String description, String specialDescription,
                            String interval, String consumption, String distance, String kind,
                            String subKind, String releaseType, String weapon) {
    }
}
