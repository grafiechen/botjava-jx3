package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.data.official.OfficialQueryData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

@Jx3Action
public class TradeItemSearchAction extends Jx3BaseAction {
    private static final int MAX_ITEMS = 8;

    public TradeItemSearchAction(ApiProperties apiProperties, Jx3RequestUtil requestUtil,
                                 GroupConfigurationService groupConfigurationService) {
        super(apiProperties, requestUtil, groupConfigurationService);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        return Map.of("name", currentArguments().get("name"));
    }

    @Override
    protected BotResponse dealAfterJx3ApiRequest(BaseResult baseResult) {
        return buildMessageByTemplate(baseResult);
    }

    @Override
    protected BotResponse.ResponseType getResponseType() {
        return BotResponse.ResponseType.TEXT;
    }

    @Override
    protected String buildTextContent(BaseResult baseResult) {
        if (baseResult == null || !(baseResult.getData() instanceof List<?> values)) {
            return "未找到相关物品。";
        }

        List<OfficialQueryData.TradeItem> items = values.stream()
                .filter(OfficialQueryData.TradeItem.class::isInstance)
                .map(OfficialQueryData.TradeItem.class::cast)
                .limit(MAX_ITEMS)
                .toList();
        if (items.isEmpty()) {
            return "未找到相关物品。";
        }

        StringBuilder content = new StringBuilder("搜索物品（显示 ")
                .append(items.size())
                .append(" 条）");
        for (int index = 0; index < items.size(); index++) {
            content.append('\n').append(formatItem(index + 1, items.get(index)));
        }
        return content.toString();
    }

    private String formatItem(int index, OfficialQueryData.TradeItem item) {
        StringJoiner fields = new StringJoiner("；");
        fields.add(index + ". " + defaultText(item.getName(), "未知物品"));
        addField(fields, "别名", combine(item.getAlias(), item.getWblalias()));
        return fields.toString();
    }

    private void addField(StringJoiner fields, String label, String value) {
        String readable = normalize(value);
        if (!readable.isEmpty()) {
            fields.add(label + "：" + readable);
        }
    }

    private String combine(String first, String second) {
        String left = normalize(first);
        String right = normalize(second);
        if (left.isEmpty()) {
            return right;
        }
        if (right.isEmpty() || left.equals(right)) {
            return left;
        }
        return left + "/" + right;
    }

    private String defaultText(String value, String fallback) {
        String readable = normalize(value);
        return readable.isEmpty() ? fallback : readable;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim();
    }
}
