package com.grafie.botjava.jx3.http.action.base;


import com.grafie.botjava.entity.GroupInfo;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.MessageInfo;
import com.grafie.botjava.jx3.http.MethodEnum;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.mapper.GroupInfoMapper;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;

/**
 * 基础类
 *
 * @author grafie.chen
 * @since 2025/1/22  16:19
 */

public abstract class Jx3BaseAction {

    protected ApiProperties apiProperties;
    protected Jx3RequestUtil jx3RequestUtil;
    protected GroupInfoMapper groupInfoMapper;
    @Value("${my.default-server}")
    private String defaultServer;
    protected GroupAtMessageCreateDto groupAtMessageCreateDto;

    public Jx3BaseAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil) {
        this.apiProperties = apiProperties;
        this.jx3RequestUtil = jx3RequestUtil;
    }

    /**
     * 需要按照不同调用方法的顺序进行传参
     *
     * @param requestRegex 请求参数
     * @param regex        方法枚举
     * @return 返回结果
     */
    public MessageInfo doRequest(GroupAtMessageCreateDto atMessageCreateDto, String requestRegex, REGEX regex) {
        this.groupAtMessageCreateDto = atMessageCreateDto;
        return doAction(requestRegex, regex.getMethodEnum());
    }

    public MessageInfo doAction(String requestRegex, MethodEnum methodEnum) {
        Map<String, Object> requestParam = getRequestParam(requestRegex);
        RequestResult requestResult = jx3RequestUtil.doPostRequest(methodEnum.getMethodPath(), requestParam);
        BaseResult baseResult = jx3RequestUtil.getResultRealData(requestResult, methodEnum);
        return dealAfterJx3ApiRequest(baseResult);
    }

    /**
     * 交给各个子类处理的生成请求参数的基类
     * @param requestRegex qq发过来的表达式
     * @return
     */
    protected abstract Map<String, Object> getRequestParam(String requestRegex);

    /**
     * 请求jx3Api之后，过来的
     * @param baseResult 需要拼装成真正返回值的内容
     * @return
     */
    protected abstract MessageInfo dealAfterJx3ApiRequest(BaseResult baseResult);

    /**
     * 设置服务
     *
     * @param requestParam 请求参数
     */

    public void getDefaultServer(Map<String, Object> requestParam) {
        if (requestParam.get("server") == null) {
            GroupInfo groupInfo = groupInfoMapper.findByOpenGroupId(groupAtMessageCreateDto.getGroupOpenid());
            String requestSever = null;
            if (groupInfo == null) {
                requestSever = defaultServer;
            } else {
                requestSever = groupInfo.getServer();
            }
            requestParam.put("server", requestSever);
        }
    }
}
