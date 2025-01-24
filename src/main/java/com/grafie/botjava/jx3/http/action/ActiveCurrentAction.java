package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.TxMessageInfo;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.data.active.ActiveCurrentData;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.mapper.GroupInfoMapper;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author grafie.chen
 * @since 2025/1/24  14:14
 */
@Jx3Action
public class ActiveCurrentAction extends Jx3BaseAction {

    public ActiveCurrentAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupInfoMapper groupInfoMapper) {
        super(apiProperties, jx3RequestUtil, groupInfoMapper);
    }

    @Override
    protected Map<String, Object> getRequestParam(String requestRegex, REGEX regex) {
        Map<String, String> valueMap = regex.handleEncounter(requestRegex);
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("server", valueMap.get("server"));
        String num = valueMap.get("value");
        if (StringUtils.isBlank(num)) {
            requestMap.put("num", Integer.parseInt(num));
        }
        return requestMap;
    }

    @Override
    protected TxMessageInfo dealAfterJx3ApiRequest(BaseResult baseResult) {
        ActiveCurrentData activeCurrentData = (ActiveCurrentData) baseResult.getData();
        String result = String.format(
                "当前时间：%s 星期%s\n" +
                        "秘境大战：%s\n" +
                        "战场任务：%s\n" +
                        "宗门任务：%s\n" +
                        "阵营任务：%s\n" +
                        "宠物奇缘：%s\n\n" +
                        "家园声望·加倍道具 \n%s\n" +
                        "武林通鉴·公共任务 \n%s\n" +
                        "武林通鉴·秘境任务 \n%s\n" +
                        "武林通鉴·团队秘境 \n%s\n",
                activeCurrentData.getDate(),
                activeCurrentData.getWeek(),
                activeCurrentData.getWar(),
                activeCurrentData.getBattle(),
                activeCurrentData.getSchool(),
                activeCurrentData.getOrecar(),
                String.join("，", activeCurrentData.getLuck()),
                String.join("，", activeCurrentData.getCard()),
                (activeCurrentData.getTeam()).get(0),
                (activeCurrentData.getTeam()).get(1),
                (activeCurrentData.getTeam()).get(2)
        );
        TxMessageInfo txMessageInfo = new TxMessageInfo();
        txMessageInfo.setMsg_type(0);
        txMessageInfo.setContent(result);
        return txMessageInfo;
    }
}
