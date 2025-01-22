package com.grafie.botjava.jx3.http.action.base;


import com.grafie.botjava.entity.GroupInfo;
import com.grafie.botjava.entity.dto.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.MethodEnum;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.util.RequestUtl;
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
    protected RequestUtl requestUtl;
    protected GroupInfoMapper groupInfoMapper;
    @Value("${my.default-server}")
    private String defaultServer;
    protected String groupOpenid;

    public Jx3BaseAction(ApiProperties apiProperties, RequestUtl requestUtl) {
        this.apiProperties = apiProperties;
        this.requestUtl = requestUtl;
    }

    /**
     * 需要按照不同调用方法的顺序进行传参
     *
     * @param requestRegex 请求参数
     * @param methodEnum   方法枚举
     * @return 返回结果
     */
    public <T> BaseResult<T> doRequest(GroupAtMessageCreateDto atMessageCreateDto, String requestRegex, MethodEnum methodEnum) {
        groupOpenid = atMessageCreateDto.getGroupOpenid();
        RequestResult requestResult = deal(requestRegex);
        return requestUtl.getResultRealData(requestResult, methodEnum);
    }

    /**
     * 具体的处理方法
     *
     * @param requestRegex 请求参数
     * @return RequestResult
     */
    protected abstract RequestResult deal(String requestRegex);

    /**
     * 设置服务
     *
     * @param requestParam 请求参数
     */

    public void getDefaultServer(Map<String, Object> requestParam) {
        if (requestParam.get("server") == null) {
            GroupInfo groupInfo = groupInfoMapper.findByOpenGroupId(groupOpenid);
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
