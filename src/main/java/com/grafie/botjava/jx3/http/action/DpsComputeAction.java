package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.data.other.DpsComputeData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import com.grafie.botjava.util.ObjectMapperUtil;
import com.grafie.botjava.util.RequestUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * dps计算
 *
 * @author grafie.chen
 * @since 2025/1/23  16:28
 */
@Slf4j
@Jx3Action
public class DpsComputeAction extends Jx3BaseAction {

    public DpsComputeAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupConfigurationService groupConfigurationService) {
        super(apiProperties, jx3RequestUtil, groupConfigurationService);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        return null;
    }

    @Override
    protected BotResponse dealAfterJx3ApiRequest(BaseResult baseResult) {
        // 组装请求角色属性的
        Map<String, Object> roleAttrRequestMap = new HashMap<>();
        roleAttrRequestMap.put("server", currentArguments().server(getDefaultServer()));
        roleAttrRequestMap.put("name", currentArguments().roleName());
        roleAttrRequestMap.put("ticket", apiProperties.getTicket());
        DpsComputeData dpsComputeData = (DpsComputeData) jx3RequestUtil
                .doPostRequest(REGEX.RoleAttribute.getMethodEnum().getMethodPath(), roleAttrRequestMap).getData();
        dpsComputeData.setBot(apiProperties.getName());
        dpsComputeData.setLoop(currentArguments().loop());
        dpsComputeData.setModel(apiProperties.getDpsModel());

        Map<String, String> headers = new HashMap<>();
        headers.put("token", apiProperties.getDpsToken());
        headers.put("Content-Type", "application/json");
        Map<String, Object> result = callDpsService(
                ObjectMapperUtil.getObjectMapper().convertValue(dpsComputeData, Map.class), headers);

        // 处理逻辑
        if (result != null && result.containsKey("code") && (Integer) result.get("code") == 200) {
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            if (data != null && data.containsKey("image")) {
                String imageUrl = (String) data.get("image");
                return BotResponse.imageUrl(imageUrl);
            }
        } else {
            log.error("DPS 计算服务返回异常，command=>{}", REGEX.DpsCompute.name());
        }
        return BotResponse.text("调用姐姐的dps计算服务出现异常");
    }

    protected Map<String, Object> callDpsService(Map<String, Object> requestBody,
                                                 Map<String, String> headers) {
        return RequestUtil.doPost(apiProperties.getDpsServiceUrl(), apiProperties.getDpsServicePath(),
                requestBody, headers, Map.class);
    }
}
