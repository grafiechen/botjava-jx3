package com.grafie.botjava.action;

import com.grafie.botjava.config.BotMessageAction;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.interaction.InteractionCreateDto;
import com.grafie.botjava.entity.dto.payload.Payload;
import com.grafie.botjava.qq.interaction.QqInteractionHandlerRegistry;
import com.grafie.botjava.service.GroupMessageSender;
import com.grafie.botjava.util.ObjectMapperUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * QQ 按钮、菜单及后续表单互动事件入口。
 */
@Slf4j
@BotMessageAction
public class InteractionCreateAction extends BaseAction {

    private final QqInteractionHandlerRegistry handlerRegistry;
    private final GroupMessageSender groupMessageSender;

    public InteractionCreateAction(QqInteractionHandlerRegistry handlerRegistry,
                                   GroupMessageSender groupMessageSender) {
        this.handlerRegistry = handlerRegistry;
        this.groupMessageSender = groupMessageSender;
    }

    @Override
    protected Object deal(Payload payload) throws Exception {
        InteractionCreateDto interaction = ObjectMapperUtil.readValue(
                payload.getD(), InteractionCreateDto.class);
        BotResponse response = handlerRegistry.dispatch(interaction);
        if (response == null) {
            Integer dataType = interaction.getData() == null ? null : interaction.getData().getType();
            log.info("QQ 互动事件暂无业务处理器，interactionType=>{}，dataType=>{}，scene=>{}",
                    interaction.getType(), dataType, interaction.getScene());
            return null;
        }
        if (isBlank(interaction.getGroupOpenId())) {
            throw new IllegalArgumentException("当前互动返回仅支持群场景，缺少 group_openid");
        }
        if (isBlank(interaction.getId())) {
            throw new IllegalArgumentException("互动事件缺少 id，无法发送事件被动回复");
        }
        groupMessageSender.sendEventReply(interaction.getGroupOpenId(), interaction.getId(), response);
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
