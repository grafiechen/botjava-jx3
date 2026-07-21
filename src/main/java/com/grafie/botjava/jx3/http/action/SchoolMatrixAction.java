package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.data.school.matirx.DescriptiveSkill;
import com.grafie.botjava.jx3.http.data.school.matirx.SchoolMatrixData;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;

@Jx3Action

/**
 * 阵法效果
 *
 * @author grafie.chen
 * @since 2025/1/22  17:17
 */
public class SchoolMatrixAction extends Jx3BaseAction {
    public SchoolMatrixAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupConfigurationService groupConfigurationService) {
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
        return "心法阵眼";
    }

    @Override
    protected Map<String, Object> buildTemplateData(BaseResult baseResult) {
        SchoolMatrixData matrix = baseResult != null && baseResult.getData() instanceof SchoolMatrixData value
                ? value : null;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", matrix == null ? currentArguments().get("name") : matrix.getName());
        data.put("skillName", matrix == null ? null : matrix.getSkillName());
        data.put("effects", toViews(matrix == null ? null : matrix.getEffects()));
        return data;
    }

    private List<EffectView> toViews(List<DescriptiveSkill> effects) {
        if (effects == null || effects.isEmpty()) {
            return List.of();
        }
        return effects.stream()
                .filter(effect -> effect != null)
                .map(effect -> new EffectView(effect.getLevel(), effect.getName(), effect.getDescription()))
                .toList();
    }

    public record EffectView(Integer level, String name, String description) {
    }
}
