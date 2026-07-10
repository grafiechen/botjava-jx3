package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.data.server.ServerCheckData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.mapper.GroupInfoMapper;
import com.grafie.botjava.entity.dto.common.TxMessageInfo;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Jx3Action

/**
 * 开服检查
 *
 * @author grafie.chen
 * @since 2025/1/22  17:18
 */
public class ServerCheckAction extends Jx3BaseAction {
    public ServerCheckAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupInfoMapper groupInfoMapper) {
        super(apiProperties, jx3RequestUtil, groupInfoMapper);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        Map<String, String> valueMap = regex.handleEncounter(requestRegex);
        Map<String, Object> requestMap = new HashMap<>();
        String server = valueMap.get("server");
        if (StringUtils.isBlank(server)){
            server = getDefaultServer();
        }
        requestMap.put("server", server);
        return requestMap;
    }

    @Override
    protected TxMessageInfo dealAfterJx3ApiRequest(BaseResult baseResult) {
        return buildMessageByTemplate(baseResult);
    }

    @Override
    protected String buildTextContent(BaseResult baseResult) {
        ServerCheckData serverCheckData = (ServerCheckData) baseResult.getData();
        return String.format(
                "服务器[%s],%s",
                serverCheckData.getServer(),
                serverCheckData.getStatus() == 1 ? "已开服" : "维护中"
        );
    }
}
