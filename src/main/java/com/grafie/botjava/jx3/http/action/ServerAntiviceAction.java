package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.entity.dto.common.TxMessageInfo;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.BaseResult;

import java.util.Map;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;

@Jx3Action

/**
 * 诛恶事件
 * @author grafie.chen
 * @since 2025/1/22  17:23
 */
public class ServerAntiviceAction extends Jx3BaseAction {
    public ServerAntiviceAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil) {
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
