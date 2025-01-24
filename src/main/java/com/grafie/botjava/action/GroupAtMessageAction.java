package com.grafie.botjava.action;

import com.grafie.botjava.config.BotMessageAction;
import com.grafie.botjava.entity.dto.common.TxMessageInfo;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.entity.dto.payload.Payload;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.util.BotRequestUtl;
import com.grafie.botjava.util.ObjectMapperUtil;
import com.grafie.botjava.util.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * @author grafie.chen
 * @since 2025/1/22  12:49
 */
@Slf4j
@BotMessageAction
@SuppressWarnings("Unchecked")
public class GroupAtMessageAction extends BaseAction {


    private final BotRequestUtl botRequestUtl;
    /**
     * 群消息发送地址
     */
    private String GROUP_MESSAGE = "/v2/groups/%s/messages";

    public GroupAtMessageAction(BotRequestUtl botRequestUtl) {
        this.botRequestUtl = botRequestUtl;
    }

    @Override
    protected Object deal(Payload payload) throws Exception {
        // 根据正则表达式，提取出来指令和内容
        GroupAtMessageCreateDto atMessageDto = ObjectMapperUtil.readValue(payload.getD(), GroupAtMessageCreateDto.class);
        REGEX regex = REGEX.matchEnum(atMessageDto.getContent());
        Jx3BaseAction jx3BaseAction
                = (Jx3BaseAction) SpringContextUtil.getBean(regex.getBaseAction());
        TxMessageInfo baseResult = jx3BaseAction.doRequest(atMessageDto, atMessageDto.getContent(), regex);
        baseResult.setMsg_id(atMessageDto.getId());
        // group_at_message 没有需要返回的数据
        Map<String, Object> base = ObjectMapperUtil.getObjectMapper().convertValue(baseResult, Map.class);
        log.info("回复群消息，http调用结果=>{}", botRequestUtl.doPost(String.format(GROUP_MESSAGE, atMessageDto.getGroupOpenid()), base));
        return null;
    }
}
