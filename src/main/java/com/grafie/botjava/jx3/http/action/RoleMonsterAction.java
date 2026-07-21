package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.data.role.monster.MonsterSkill;
import com.grafie.botjava.jx3.http.data.role.monster.RoleMonsterData;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;


/**
 * 百战精耐
 * @author grafie.chen
 * @since 2025/1/22  17:28
 */
@Jx3Action
public class RoleMonsterAction extends Jx3BaseAction {

    public RoleMonsterAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupConfigurationService groupConfigurationService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        return Map.of(
                "server", currentArguments().server(getDefaultServer()),
                "name", currentArguments().roleName()
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
        return "角色百战";
    }

    @Override
    protected Map<String, Object> buildTemplateData(BaseResult baseResult) {
        RoleMonsterData role = baseResult != null && baseResult.getData() instanceof RoleMonsterData value
                ? value : null;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("zone", role == null ? null : role.getZone());
        data.put("server", role == null ? currentArguments().server(getDefaultServer()) : role.getServer());
        data.put("roleName", role == null ? currentArguments().roleName() : role.getRoleName());
        data.put("skillEnergy", role == null ? null : role.getSkillEnergy());
        data.put("skillStamina", role == null ? null : role.getSkillStamina());
        data.put("skillCount", role == null ? null : role.getSkillCount());
        data.put("updateTime", role == null ? null : role.getUpdateTime());
        data.put("skills", toViews(role == null ? null : role.getSkillList()));
        return data;
    }

    private List<SkillView> toViews(List<MonsterSkill> skills) {
        if (skills == null || skills.isEmpty()) {
            return List.of();
        }
        return skills.stream()
                .filter(skill -> skill != null && skill.getSkillName() != null && !skill.getSkillName().isBlank())
                .map(skill -> new SkillView(
                        skill.getSkillName(), skill.getLeaderName(), skill.getCost(),
                        skill.getColor(), skill.getLevel(), Boolean.TRUE.equals(skill.getDeprecated())))
                .toList();
    }

    public record SkillView(String name, String leaderName, Integer cost,
                            Integer color, Integer level, boolean deprecated) {
    }
}
