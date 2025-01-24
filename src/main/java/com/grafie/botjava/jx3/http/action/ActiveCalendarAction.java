package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.config.Jx3Action;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.entity.dto.common.TxMessageInfo;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 活动日历
 * 只有 星期三、星期五、星期六、星期日 才有美人画图，星期三、星期五 才有世界首领，若非活动时间不返回相关键与值。
 *
 * @author grafie.chen
 * @since 2025/1/22  17:15
 */
@Jx3Action
public class ActiveCalendarAction extends Jx3BaseAction {

    public ActiveCalendarAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil) {
        super(apiProperties, jx3RequestUtil);
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
        System.out.println(baseResult);
        return null;
    }
}
