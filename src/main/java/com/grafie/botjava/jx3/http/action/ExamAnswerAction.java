package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.data.exam.ExamAnswerData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;


/**
 * 科举试题
 * 说明:
 * 搜索科举试题的答案
 * @author grafie.chen
 * @since 2025/1/22  17:16
 */
@Jx3Action
public class ExamAnswerAction extends Jx3BaseAction {
    public ExamAnswerAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupConfigurationService groupConfigurationService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        return Map.of("subject", currentArguments().get("subject"), "limit", 5);
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
        return "科举答题";
    }

    @Override
    protected Map<String, Object> buildTemplateData(BaseResult baseResult) {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("subject", currentArguments().get("subject"));
        template.put("data", toViews(baseResult == null ? null : baseResult.getData()));
        return template;
    }

    private List<QuestionView> toViews(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        List<QuestionView> views = new ArrayList<>();
        for (Object item : values) {
            if (item instanceof ExamAnswerData answer) {
                views.add(new QuestionView(answer.getQuestion(), answer.getAnswer()));
            }
        }
        return List.copyOf(views);
    }

    public record QuestionView(String question, String answer) {
    }
}
