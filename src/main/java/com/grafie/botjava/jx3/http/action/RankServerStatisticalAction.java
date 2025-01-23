package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.jx3.http.MessageInfo;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;


/**
 * 全服榜单
 * 各个table的数据结构不同，最终数据以返回为准。
 *  @author grafie.chen
 * @since 2025/1/22  17:25
 */
@Jx3Action
public class RankServerStatisticalAction extends Jx3BaseAction {
    public RankServerStatisticalAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil) {
        super(apiProperties, jx3RequestUtil);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex) {
        return null;
    }
}
