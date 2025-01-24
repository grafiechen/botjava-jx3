package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.TxFileUploadResultDto;
import com.grafie.botjava.entity.dto.common.MediaDto;
import com.grafie.botjava.entity.dto.common.TxMessageInfo;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.data.other.DpsComputeData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.mapper.GroupInfoMapper;
import com.grafie.botjava.util.BotRequestUtl;
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
    private final BotRequestUtl botRequestUtl;

    public DpsComputeAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupInfoMapper groupInfoMapper, BotRequestUtl botRequestUtl) {
        super(apiProperties, jx3RequestUtil, groupInfoMapper);
        this.botRequestUtl = botRequestUtl;
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        return null;
    }

    @Override
    protected TxMessageInfo dealAfterJx3ApiRequest(BaseResult baseResult) {
        Map<String, String> valueMap = regex.handleEncounter(requestRegex);
        // 组装请求角色属性的
        Map<String, Object> roleAttrRequestMap = new HashMap<>();
        roleAttrRequestMap.put("server", valueMap.get("server"));
        roleAttrRequestMap.put("name", valueMap.get("roleName"));
        roleAttrRequestMap.put("ticket", apiProperties.getTicket());
        DpsComputeData dpsComputeData = (DpsComputeData) jx3RequestUtil
                .doPostRequest(REGEX.RoleAttribute.getMethodEnum().getMethodPath(), roleAttrRequestMap).getData();
        dpsComputeData.setBot(apiProperties.getName());
        dpsComputeData.setLoop(valueMap.get("loop"));
        // 先写死
        dpsComputeData.setModel("旗舰");

        Map<String, String> headers = new HashMap<>();
        headers.put("token", apiProperties.getApiToken());
        headers.put("Content-Type", "application/json");
        Map<String, Object> result = RequestUtil.doPost("https://www.jx3hps.com", "/dps", ObjectMapperUtil.getObjectMapper().convertValue(dpsComputeData, Map.class), headers, Map.class);

        // 处理逻辑
        if (result.containsKey("code") && (Integer) result.get("code") == 200) {
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            if (data != null && data.containsKey("image")) {
                String imageUrl = (String) data.get("image");
                TxFileUploadResultDto txFileUploadResultDto =
                        botRequestUtl.doPostForUploadFile(imageUrl,
                                String.format(BotRequestUtl.fileUploadUrl, groupAtMessageCreateDto.getGroupOpenid()), 1);
                TxMessageInfo txMessageInfo = new TxMessageInfo();
                txMessageInfo.setMsg_type(7);
                txMessageInfo.setMedia(new MediaDto(txFileUploadResultDto.getFileInfo()));
                txMessageInfo.setContent(" ");
                return txMessageInfo;
            }
        } else {
            log.error("Code is not 200 or data missing.");
        }
        TxMessageInfo txMessageInfo = new TxMessageInfo();
        txMessageInfo.setMsg_type(0);
        txMessageInfo.setContent("调用姐姐的dps计算服务出现异常");
        return txMessageInfo;
    }
}
