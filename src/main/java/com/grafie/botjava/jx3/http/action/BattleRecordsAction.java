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
import com.grafie.botjava.util.TimeUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 帮战记录查询与图片数据拼装。
 */
@Jx3Action
public class BattleRecordsAction extends Jx3BaseAction {

    public BattleRecordsAction(ApiProperties apiProperties, Jx3RequestUtil requestUtil,
                               GroupConfigurationService groupConfigurationService) {
        super(apiProperties, requestUtil, groupConfigurationService);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        return Map.of("server", currentArguments().server(getDefaultServer()));
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
        return "帮战记录";
    }

    @Override
    protected Object buildTemplateData(BaseResult baseResult) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("server", currentArguments().server(getDefaultServer()));
        data.put("data", toViews(baseResult == null ? null : baseResult.getData()));
        return data;
    }

    private List<BattleRecordView> toViews(Object responseData) {
        if (!(responseData instanceof List<?> records)) {
            return List.of();
        }
        return records.stream()
                .filter(OfficialQueryData.BattleRecord.class::isInstance)
                .map(OfficialQueryData.BattleRecord.class::cast)
                .map(this::toView)
                .toList();
    }

    private BattleRecordView toView(OfficialQueryData.BattleRecord record) {
        return new BattleRecordView(
                record.getZoneName(),
                record.getServerName(),
                record.getDeclaringTongName(),
                record.getAcceptingTongName(),
                formatTime(record.getStartTime()),
                formatTime(record.getEndTime()),
                formatDuration(record.getMatchDuration())
        );
    }

    private String formatTime(Long timestamp) {
        return timestamp == null ? null : TimeUtils.timeFormatting(timestamp);
    }

    private String formatDuration(Long seconds) {
        if (seconds == null || seconds <= 0) {
            return null;
        }
        long hours = seconds / 3600;
        long minutes = seconds % 3600 / 60;
        long remainingSeconds = seconds % 60;
        StringBuilder result = new StringBuilder();
        if (hours > 0) {
            result.append(hours).append("小时");
        }
        if (minutes > 0) {
            result.append(minutes).append("分钟");
        }
        if (result.isEmpty() && remainingSeconds > 0) {
            result.append(remainingSeconds).append("秒");
        }
        return result.toString();
    }

    public record BattleRecordView(
            String zoneName,
            String serverName,
            String declaringTongName,
            String acceptingTongName,
            String startTime,
            String endTime,
            String duration
    ) {
    }
}
