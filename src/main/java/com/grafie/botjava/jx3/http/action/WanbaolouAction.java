package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.data.trade.WanbaolouData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 按角色编号查询万宝楼账号详情。
 */
@Jx3Action
public class WanbaolouAction extends Jx3BaseAction {
    private static final Pattern DETAIL_PATTERN = Pattern.compile("^【([^】]+)】\\s*(.*)$");
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("MM/dd HH:mm").withZone(ZoneId.of("Asia/Shanghai"));

    public WanbaolouAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil,
                           GroupConfigurationService groupConfigurationService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        return Map.of("id", currentArguments().value());
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
        return "万宝楼";
    }

    @Override
    protected Object buildTemplateData(BaseResult baseResult) {
        Map<String, Object> view = new LinkedHashMap<>();
        WanbaolouData data = baseResult != null && baseResult.getData() instanceof WanbaolouData value
                ? value : new WanbaolouData();
        view.put("data", data);
        view.put("details", parseDetails(data.getReplyContent()));
        view.put("tradeStatusText", tradeStatusText(data.getTradeStatus()));
        view.put("replyTimeText", formatTime(data.getReplyTime()));
        return view;
    }

    static List<DetailRow> parseDetails(String replyContent) {
        List<DetailRow> rows = new ArrayList<>();
        if (replyContent == null || replyContent.isBlank()) {
            return rows;
        }
        for (String rawLine : replyContent.split("(?i)<br\\s*/?>")) {
            String line = HTML_TAG_PATTERN.matcher(rawLine).replaceAll("").trim();
            if (line.isEmpty()) {
                continue;
            }
            Matcher matcher = DETAIL_PATTERN.matcher(line);
            if (matcher.matches()) {
                rows.add(new DetailRow(matcher.group(1).trim(), matcher.group(2).trim()));
            } else if (!rows.isEmpty()) {
                DetailRow previous = rows.remove(rows.size() - 1);
                rows.add(new DetailRow(previous.label(), previous.content() + "\n" + line));
            } else {
                rows.add(new DetailRow("详情", line));
            }
        }
        return List.copyOf(rows);
    }

    static String tradeStatusText(Integer status) {
        if (status == null) {
            return "状态未知";
        }
        return switch (status) {
            case 1 -> "在售";
            case 2 -> "已售";
            case 3 -> "公示";
            case 4 -> "下架";
            default -> "状态 " + status;
        };
    }

    static String formatTime(Long timestamp) {
        if (timestamp == null || timestamp <= 0) {
            return "时间未知";
        }
        return TIME_FORMATTER.format(Instant.ofEpochSecond(timestamp));
    }

    public record DetailRow(String label, String content) {
    }
}