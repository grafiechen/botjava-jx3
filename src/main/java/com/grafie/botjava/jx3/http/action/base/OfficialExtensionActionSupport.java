package com.grafie.botjava.jx3.http.action.base;

import com.fasterxml.jackson.databind.JsonNode;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.data.official.FlexibleOfficialData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.service.GroupConfigurationService;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenAPI 未提供稳定深层 schema 的简短文本输出辅助。
 */
public abstract class OfficialExtensionActionSupport extends Jx3BaseAction {

    protected OfficialExtensionActionSupport(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil,
                                             GroupConfigurationService groupConfigurationService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
    }

    protected BotResponse fieldText(String key, BaseResult baseResult) {
        FlexibleOfficialData data = flexibleData(baseResult);
        String value = data == null ? null : data.text(key);
        return BotResponse.text(value == null || value.isBlank() ? "暂无可展示数据。" : value);
    }

    protected BotResponse answerText(BaseResult baseResult) {
        FlexibleOfficialData data = flexibleData(baseResult);
        if (data == null) {
            return BotResponse.text("答案之书：暂无答案。");
        }
        String answer = data.text("answer");
        String hearten = data.text("hearten");
        if (answer == null || answer.isBlank()) {
            return BotResponse.text("答案之书：暂无答案。");
        }
        return BotResponse.text(hearten == null || hearten.isBlank() ? answer : answer + "\n" + hearten);
    }

    protected BotResponse choicesText(String prefix, BaseResult baseResult) {
        if (baseResult == null || !(baseResult.getData() instanceof List<?> values) || values.isEmpty()) {
            return BotResponse.text(prefix + "暂无推荐。");
        }
        List<String> choices = values.stream()
                .filter(value -> value != null && !value.toString().isBlank())
                .limit(5)
                .map(Object::toString)
                .toList();
        return BotResponse.text(choices.isEmpty() ? prefix + "暂无推荐。" : prefix + String.join("、", choices));
    }

    protected BotResponse rankText(String title, BaseResult baseResult) {
        FlexibleOfficialData root = flexibleData(baseResult);
        if (root == null) {
            return BotResponse.text(title + "：暂无数据。");
        }
        List<String> lines = new ArrayList<>();
        String name = root.text("name");
        String server = root.text("server");
        lines.add((name == null || name.isBlank()) ? title : name);
        if (server != null && !server.isBlank()) {
            lines.add("服务器：" + server);
        }
        JsonNode records = root.get("data");
        if (records != null && records.isArray()) {
            lines.add("记录数：" + records.size());
            int limit = Math.min(5, records.size());
            for (int index = 0; index < limit; index++) {
                JsonNode item = records.get(index);
                String itemName = firstText(item, "corpsName", "tongName", "name");
                String score = firstText(item, "corpsLevel", "totalScore", "score", "money");
                if (itemName != null) {
                    lines.add((index + 1) + ". " + itemName + (score == null ? "" : "：" + score));
                }
            }
        }
        return BotResponse.text(String.join("\n", lines));
    }

    private String firstText(JsonNode item, String... names) {
        if (item == null) {
            return null;
        }
        for (String name : names) {
            JsonNode value = item.get(name);
            if (value != null && !value.isNull() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return null;
    }

    private FlexibleOfficialData flexibleData(BaseResult baseResult) {
        return baseResult != null && baseResult.getData() instanceof FlexibleOfficialData data ? data : null;
    }
}