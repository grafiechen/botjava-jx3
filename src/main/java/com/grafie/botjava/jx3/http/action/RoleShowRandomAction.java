package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.jx3.http.MessageInfo;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;


/**
 * 角色名片
 * 客户端的名片墙。
 * @author grafie.chen
 * @since 2025/1/22  17:29
 */
@Jx3Action
public class RoleShowRandomAction extends Jx3BaseAction {
    public RoleShowRandomAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil) {
        super(apiProperties, jx3RequestUtil);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex) {
        return null;
    }
}
