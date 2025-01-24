package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.GroupInfo;
import com.grafie.botjava.entity.dto.common.TxMessageInfo;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.mapper.GroupInfoMapper;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * @author grafie.chen
 * @since 2025/1/23  16:59
 */
@Jx3Action
public class BindServerAction extends Jx3BaseAction {
    public BindServerAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupInfoMapper groupInfoMapper) {
        super(apiProperties, jx3RequestUtil, groupInfoMapper);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        return null;
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    protected TxMessageInfo dealAfterJx3ApiRequest(BaseResult baseResult) {
        Map<String, String> valueMap = regex.handleEncounter(requestRegex);
        GroupInfo groupInfo = groupInfoMapper.findByOpenGroupId(groupAtMessageCreateDto.getGroupOpenid());
        TxMessageInfo txMessageInfo = new TxMessageInfo();
        txMessageInfo.setMsg_type(0);
        String result = null;
        if (StringUtils.isNotBlank(groupInfo.getServer())) {
            result = String.format(
                    "默认服务器已存在，无法重复设置，当前设置为[%s]",
                    groupInfo.getServer()
            );
        } else {
            groupInfo.setServer(valueMap.get("server"));
            groupInfoMapper.save(groupInfo);
            result = String.format(
                    "默认服务器设置成功，[%s]",
                    groupInfo.getServer()
            );
        }
        txMessageInfo.setContent(result);
        return txMessageInfo;
    }
}
