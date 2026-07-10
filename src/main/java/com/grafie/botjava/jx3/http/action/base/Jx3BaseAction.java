package com.grafie.botjava.jx3.http.action.base;


import com.grafie.botjava.entity.GroupInfo;
import com.grafie.botjava.entity.dto.TxFileUploadResultDto;
import com.grafie.botjava.entity.dto.common.MediaDto;
import com.grafie.botjava.entity.dto.common.TxMessageInfo;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.BaseResult;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.mapper.GroupInfoMapper;
import com.grafie.botjava.minio.MinioUtil;
import com.grafie.botjava.util.BotRequestUtl;
import com.grafie.botjava.util.HtmlToImageUtl;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

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
    @Autowired(required = false)
    protected BotRequestUtl botRequestUtl;
    @Autowired(required = false)
    protected MinioUtil minioUtil;

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
            return buildResponse(null);
        }
        Map<String, Object> requestParam = getRequestParam(requestRegex, regex);
        RequestResult requestResult = jx3RequestUtil.doPostRequest(regex.getMethodEnum().getMethodPath(), requestParam);
        BaseResult baseResult = jx3RequestUtil.getResultRealData(requestResult, regex.getMethodEnum());
        return buildResponse(baseResult);
    }

    private TxMessageInfo buildResponse(BaseResult baseResult) {
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

    protected TxMessageInfo buildMessageByTemplate(BaseResult baseResult) {
        if (getResponseType() == ResponseType.IMAGE) {
            return buildImageMessage(baseResult);
        }
        return buildTextMessage(buildTextContent(baseResult));
    }

    protected ResponseType getResponseType() {
        return ResponseType.TEXT;
    }

    protected String buildTextContent(BaseResult baseResult) {
        throw new UnsupportedOperationException("请在子类中实现文本内容拼装");
    }

    protected String getTemplatePath() {
        throw new UnsupportedOperationException("请在子类中指定 HTML 模板路径");
    }

    protected Object buildTemplateData(BaseResult baseResult) {
        throw new UnsupportedOperationException("请在子类中实现 Vue 模板数据拼装");
    }

    protected TxMessageInfo buildTextMessage(String content) {
        TxMessageInfo txMessageInfo = new TxMessageInfo();
        txMessageInfo.setMsg_type(0);
        txMessageInfo.setContent(content);
        return txMessageInfo;
    }

    protected TxMessageInfo buildImageMessage(BaseResult baseResult) {
        if (botRequestUtl == null || minioUtil == null) {
            throw new IllegalStateException("图片消息需要 BotRequestUtl 和 MinioUtil");
        }
        try {
            Path outputPath = Files.createTempFile("jx3-bot-", ".png");
            HtmlToImageUtl.renderVueTemplateToImage(getTemplatePath(), buildTemplateData(baseResult), outputPath.toString());
            String imageUrl = minioUtil.uploadFile(outputPath.toFile(), UUID.randomUUID() + ".png");
            TxFileUploadResultDto uploadResult = botRequestUtl.doPostForUploadFile(
                    imageUrl,
                    String.format(BotRequestUtl.fileUploadUrl, groupAtMessageCreateDto.getGroupOpenid()),
                    1
            );
            TxMessageInfo txMessageInfo = new TxMessageInfo();
            txMessageInfo.setMsg_type(7);
            txMessageInfo.setContent(" ");
            txMessageInfo.setMedia(new MediaDto(uploadResult.getFileInfo()));
            return txMessageInfo;
        } catch (Exception e) {
            throw new RuntimeException("生成图片消息失败", e);
        }
    }

    public enum ResponseType {
        TEXT,
        IMAGE
    }

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
