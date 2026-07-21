package com.grafie.botjava.qq.interaction;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.interaction.InteractionCreateDto;

/**
 * 单个 QQ 按钮、菜单或表单交互的业务扩展点。
 */
public interface QqInteractionHandler {

    boolean supports(InteractionCreateDto interaction);

    BotResponse handle(InteractionCreateDto interaction);
}
