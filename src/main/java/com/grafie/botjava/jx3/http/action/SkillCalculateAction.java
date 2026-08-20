package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.action.base.OfficialExtensionActionSupport;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import com.grafie.botjava.jx3.http.command.CommandArgumentException;
import com.grafie.botjava.jx3.http.data.official.SkillCalculateData;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Jx3Action
public class SkillCalculateAction extends OfficialExtensionActionSupport {
    public SkillCalculateAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil,
                     GroupConfigurationService groupConfigurationService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        String value = currentArguments().get("cooldown");
        double cooldown = 1.5D;
        if (value != null) {
            try {
                cooldown = Double.parseDouble(value);
            } catch (NumberFormatException exception) {
                throw new CommandArgumentException("技能 CD 必须是数字");
            }
        }
        if (cooldown <= 0 || cooldown > 60) {
            throw new CommandArgumentException("技能 CD 必须大于 0 且不超过 60 秒");
        }
        return Map.of("cooldown", cooldown);
    }

    @Override
    protected BotResponse dealAfterJx3ApiRequest(BaseResult baseResult) {
        if (baseResult == null || !(baseResult.getData() instanceof List<?> values) || values.isEmpty()) {
            return BotResponse.text("急速计算：暂无数据。");
        }
        List<String> lines = new ArrayList<>();
        lines.add("急速档位（前 6 条）：");
        values.stream().filter(SkillCalculateData.class::isInstance)
                .map(SkillCalculateData.class::cast).limit(6)
                .forEach(value -> lines.add("急速 " + value.getLevel()
                        + "，CD " + value.getDuration() + " 秒，帧数 " + value.getNowFrame()));
        return BotResponse.text(String.join("\n", lines));
    }
}