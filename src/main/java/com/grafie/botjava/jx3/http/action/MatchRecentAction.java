package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.entity.dto.common.TxMessageInfo;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.BaseResult;

import java.util.Map;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.mapper.GroupInfoMapper;


/**
 * 名剑战绩,角色近期战绩记录
 *
 * @author grafie.chen
 * @since 2025/1/22  17:20
 */
@Jx3Action
public class MatchRecentAction extends Jx3BaseAction {
    public MatchRecentAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupInfoMapper groupInfoMapper) {
        super(apiProperties, jx3RequestUtil, groupInfoMapper);
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
