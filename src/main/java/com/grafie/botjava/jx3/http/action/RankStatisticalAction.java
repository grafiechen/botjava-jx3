package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.data.official.OfficialQueryData;
import com.grafie.botjava.util.TimeUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;


/**
 * 风云榜单
 * @author grafie.chen
 * @since 2025/1/22  17:21
 */
@Jx3Action
public class RankStatisticalAction extends Jx3BaseAction {
    public RankStatisticalAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupConfigurationService groupConfigurationService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        return Map.of(
                "server", currentArguments().server(getDefaultServer()),
                "name", firstNotBlank(currentArguments().get("name1"), currentArguments().get("name"))
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
        return "本服榜单";
    }

    @Override
    protected Object buildTemplateData(BaseResult baseResult) {
        Object responseData = baseResult == null ? null : baseResult.getData();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("server", currentArguments().server(getDefaultServer()));
        data.put("name", firstNotBlank(currentArguments().get("name1"), currentArguments().get("name")));
        data.put("time", resolveReadableTime(responseData));
        data.put("data", responseData);
        return data;
    }

    private String resolveReadableTime(Object responseData) {
        if (!(responseData instanceof OfficialQueryData.RankStatistical ranking)
                || ranking.getTime() == null) {
            return null;
        }
        return TimeUtils.timeFormatting(ranking.getTime());
    }
}
