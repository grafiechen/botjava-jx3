package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.jx3.http.MessageInfo;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;


/**
 *智障聊天
 * @author grafie.chen
 * @since 2025/1/22  17:29
 */
@Jx3Action
public class ChatMixedAction extends Jx3BaseAction {
    public ChatMixedAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil) {
        super(apiProperties, jx3RequestUtil);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex) {
        return null;
    }
}
