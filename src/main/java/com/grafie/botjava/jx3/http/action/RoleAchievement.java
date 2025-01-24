package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.entity.dto.common.TxMessageInfo;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.BaseResult;

import java.util.Map;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;


/**
 * 成就百科 角色成就进度
 *
 * @author grafie.chen
 * @since 2025/1/22  17:22
 */
@Jx3Action
public class RoleAchievement extends Jx3BaseAction  {
    public RoleAchievement(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil) {
        super(apiProperties, jx3RequestUtil);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        return null;
    }

    @Override
    protected TxMessageInfo dealAfterJx3ApiRequest(BaseResult baseResult) {
        return null;
    }
}
