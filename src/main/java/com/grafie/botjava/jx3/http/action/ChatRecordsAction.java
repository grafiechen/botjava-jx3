package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.data.official.ChatRecordsData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import com.grafie.botjava.util.TimeUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Jx3Action
public class ChatRecordsAction extends Jx3BaseAction {
    private static final int MAX_DISPLAY_RECORDS = 20;

    public ChatRecordsAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil,
                             GroupConfigurationService groupConfigurationService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        return Map.of(
                "server", currentArguments().get("server"),
                "name", currentArguments().get("roleName"),
                "limit", currentArguments().integer("limit", 1, MAX_DISPLAY_RECORDS).orElse(MAX_DISPLAY_RECORDS),
                "page", currentArguments().integer("page", 1, 10000).orElse(1));
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
        return "角色聊天";
    }

    @Override
    protected Map<String, Object> buildTemplateData(BaseResult baseResult) {
        ChatRecordsData source = baseResult != null && baseResult.getData() instanceof ChatRecordsData data
                ? data
                : null;
        List<ChatRecordView> records = toViews(source);
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("server", currentArguments().get("server"));
        template.put("roleName", currentArguments().get("roleName"));
        template.put("page", currentArguments().integer("page", 1, 10000).orElse(1));
        template.put("total", source == null || source.getTotal() == null ? records.size() : source.getTotal());
        template.put("data", records);
        return template;
    }

    private List<ChatRecordView> toViews(ChatRecordsData source) {
        if (source == null || source.getList() == null) {
            return List.of();
        }
        return source.getList().stream()
                .filter(record -> record != null)
                .limit(MAX_DISPLAY_RECORDS)
                .map(record -> new ChatRecordView(
                        normalize(record.getZone()),
                        normalize(record.getServer()),
                        normalize(record.getRoleName()),
                        normalize(record.getChannel()),
                        normalize(record.getMessage()),
                        formatTime(record.getTime())))
                .toList();
    }

    private String formatTime(Long timestamp) {
        if (timestamp == null) {
            return "";
        }
        long seconds = timestamp > 100_000_000_000L ? timestamp / 1000 : timestamp;
        return TimeUtils.timeFormatting(seconds);
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    public record ChatRecordView(String zone, String server, String roleName,
                                 String channel, String message, String time) {
    }
}
