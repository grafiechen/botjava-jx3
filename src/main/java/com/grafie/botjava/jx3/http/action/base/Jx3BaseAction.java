package com.grafie.botjava.jx3.http.action.base;


import com.grafie.botjava.entity.GroupInfo;
import com.grafie.botjava.entity.dto.common.TxMessageInfo;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.mapper.GroupInfoMapper;

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
    protected GroupAtMessageCreateDto groupAtMessageCreateDto;
    protected REGEX regex;
    protected String requestRegex;

    public Jx3BaseAction(ApiProperties apiProperties, Jx3RequestUtil jx3RequestUtil, GroupInfoMapper groupInfoMapper) {
        this.apiProperties = apiProperties;
        this.jx3RequestUtil = jx3RequestUtil;
        this.groupInfoMapper = groupInfoMapper;
    }

    /**
     * 需要按照不同调用方法的顺序进行传参
     *
     * @param requestRegex 请求参数
     * @param regex        方法枚举
     * @return 返回结果
     */
    public TxMessageInfo doRequest(GroupAtMessageCreateDto atMessageCreateDto, String requestRegex, REGEX regex) {
        this.groupAtMessageCreateDto = atMessageCreateDto;
        this.requestRegex = requestRegex;
        this.regex = regex;
        return doAction(requestRegex, regex);
    }

    public TxMessageInfo doAction(String requestRegex, REGEX regex) {
        // 当method为空时，说明不需要调用外部接口。直接在具体实现类里面进行处理即可
        if (regex.getMethodEnum() == null) {
            return dealAfterJx3ApiRequest(null);
        }
        Map<String, Object> requestParam = getRequestParam(requestRegex, regex);
        RequestResult requestResult = jx3RequestUtil.doPostRequest(regex.getMethodEnum().getMethodPath(), requestParam);
        BaseResult baseResult = jx3RequestUtil.getResultRealData(requestResult, regex.getMethodEnum());
        return dealAfterJx3ApiRequest(baseResult);
    }

    /**
     * 交给各个子类处理的生成请求参数的基类
     *
     * @param requestRegex qq发过来的表达式
     * @param regex        识别到的枚举
     * @return
     */
    protected abstract Map<String, Object> getRequestParam(String requestRegex, REGEX regex);

    /**
     * 请求jx3Api之后，过来的
     *
     * @param baseResult 需要拼装成真正返回值的内容
     * @return
     */
    protected abstract TxMessageInfo dealAfterJx3ApiRequest(BaseResult baseResult);

    /**
     * 设置区服
     */

    public String getDefaultServer() {
        GroupInfo groupInfo = groupInfoMapper.findByOpenGroupId(groupAtMessageCreateDto.getGroupOpenid());
        String requestSever = null;
        if (groupInfo == null) {
            requestSever = apiProperties.getDefaultServer();
        } else {
            requestSever = groupInfo.getServer();
        }
        return requestSever;
    }
}
