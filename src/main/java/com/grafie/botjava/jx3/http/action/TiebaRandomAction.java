package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.data.tieba.TiebaRandomData;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;

@Jx3Action

/**
 * 八卦帖子
 * @author grafie.chen
 * @since 2025/1/22  17:23
 */
public class TiebaRandomAction extends Jx3BaseAction {
    public TiebaRandomAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupConfigurationService groupConfigurationService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("tags", currentArguments().get("tags"));
        String server = currentArguments().get("server");
        if (server != null) {
            params.put("server", server);
        }
        params.put("limit", 1);
        return params;
    }

    @Override
    protected BotResponse dealAfterJx3ApiRequest(BaseResult baseResult) {
        TiebaRandomData post = firstPost(baseResult == null ? null : baseResult.getData());
        if (post == null) {
            return BotResponse.text("八卦帖子：暂无数据。");
        }
        StringBuilder content = new StringBuilder("八卦帖子");
        append(content, "分类", firstNotBlank(post.getTags(), currentArguments().get("tags")));
        append(content, "区服", joinPlace(post.getZone(), post.getServer()));
        append(content, "角色", post.getName());
        append(content, "标题", post.getTitle());
        append(content, "日期", post.getDate());
        return BotResponse.text(content.toString());
    }

    private TiebaRandomData firstPost(Object value) {
        if (!(value instanceof List<?> values)) {
            return null;
        }
        return values.stream()
                .filter(TiebaRandomData.class::isInstance)
                .map(TiebaRandomData.class::cast)
                .findFirst()
                .orElse(null);
    }

    private String joinPlace(String zone, String server) {
        if (zone == null || zone.isBlank()) {
            return server;
        }
        if (server == null || server.isBlank()) {
            return zone;
        }
        return zone + " / " + server;
    }

    private void append(StringBuilder content, String label, String value) {
        if (value != null && !value.isBlank()) {
            content.append('\n').append(label).append('：').append(value.trim());
        }
    }
}
