package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;

import java.util.Map;

/**
 * 群管理员通过统一主动消息链发布群公告。
 */
@Jx3Action
public class GroupAnnouncementAction extends Jx3BaseAction {

    public GroupAnnouncementAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil,
                                   GroupConfigurationService groupConfigurationService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        return Map.of();
    }

    @Override
    protected BotResponse dealAfterJx3ApiRequest(BaseResult baseResult) {
        String content = currentArguments().get("text");
        if (content == null) {
            return BotResponse.text("群公告内容不能为空。");
        }
        return BotResponse.text(content).asActiveMessage();
    }
}
