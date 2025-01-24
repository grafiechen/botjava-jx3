package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.TxMessageInfo;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.mapper.GroupInfoMapper;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;


/**
 * 装备属性
 *
 * @author grafie.chen
 * @since 2025/1/22  17:22
 */
@Jx3Action
public class RoleAttributeAction extends Jx3BaseAction {
    public RoleAttributeAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupInfoMapper groupInfoMapper) {
        super(apiProperties, jx3RequestUtil, groupInfoMapper);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        Map<String, String> valueMap = regex.handleEncounter(requestRegex);
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("server", valueMap.get("server"));
        String name = valueMap.get("value");
        if (StringUtils.isBlank(name)) {
            name = valueMap.get("value1");
        }
        requestMap.put("name", name);
        requestMap.put("ticket", apiProperties.getTicket());
        return requestMap;
    }

    @Override
    protected TxMessageInfo dealAfterJx3ApiRequest(BaseResult baseResult) {
        return null;
    }
}
