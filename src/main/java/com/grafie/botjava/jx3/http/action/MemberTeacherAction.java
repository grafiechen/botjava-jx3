package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.data.member.teacher.MemberTeacherData;
import com.grafie.botjava.jx3.http.data.member.teacher.TeacherInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;


/**
 * 师父列表
 * @author grafie.chen
 * @since 2025/1/22  17:24
 */
@Jx3Action
public class MemberTeacherAction extends Jx3BaseAction {
    public MemberTeacherAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupConfigurationService groupConfigurationService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        java.util.HashMap<String, Object> params = new java.util.HashMap<>();
        params.put("type", currentArguments().integerOneOf("type", 1, 2).orElse(1));
        params.put("server", currentArguments().server(getDefaultServer()));
        if (currentArguments().get("keyword") != null) {
            params.put("keyword", currentArguments().get("keyword"));
        }
        return params;
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
        return "师徒系统";
    }

    @Override
    protected Map<String, Object> buildTemplateData(BaseResult baseResult) {
        MemberTeacherData result = (MemberTeacherData) baseResult.getData();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("zone", result.getZone());
        data.put("server", firstNotBlank(result.getServer(), currentArguments().server(getDefaultServer())));
        data.put("time", result.getTime());
        data.put("data", toViews(result.getData()));
        return data;
    }

    private List<MentorView> toViews(List<TeacherInfo> values) {
        if (values == null) {
            return List.of();
        }
        List<MentorView> views = new ArrayList<>();
        for (TeacherInfo item : values) {
            if (item == null) {
                continue;
            }
            views.add(new MentorView(
                    item.getRoleName(), item.getRoleLevel(), item.getCampName(), item.getTongName(),
                    item.getTongMasterName(), item.getBodyName(), item.getForceName(), item.getComment()));
        }
        return List.copyOf(views);
    }

    public record MentorView(String roleName, Integer roleLevel, String campName,
                             String tongName, String tongMasterName, String bodyName,
                             String forceName, String comment) {
    }
}
