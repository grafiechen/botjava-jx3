package com.grafie.botjava.qq.interaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.interaction.InteractionCreateDto;
import com.grafie.botjava.entity.dto.interaction.InteractionDataDto;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.HelpMenuService;
import org.springframework.stereotype.Component;

/**
 * 处理帮助菜单中的分类按钮，不执行任意回调文本。
 */
@Component
public class HelpInteractionHandler implements QqInteractionHandler {

    private static final int INLINE_KEYBOARD_CLICK = 11;

    private final HelpMenuService helpMenuService;

    public HelpInteractionHandler(HelpMenuService helpMenuService) {
        this.helpMenuService = helpMenuService;
    }

    @Override
    public boolean supports(InteractionCreateDto interaction) {
        InteractionDataDto data = interaction == null ? null : interaction.getData();
        return data != null
                && Integer.valueOf(INLINE_KEYBOARD_CLICK).equals(data.getType())
                && callbackData(data).startsWith(HelpMenuService.CALLBACK_PREFIX);
    }

    @Override
    public BotResponse handle(InteractionCreateDto interaction) {
        String callbackData = callbackData(interaction.getData());
        String groupName = callbackData.substring(HelpMenuService.CALLBACK_PREFIX.length());
        REGEX.CommandGroup group;
        try {
            group = REGEX.CommandGroup.valueOf(groupName);
        } catch (IllegalArgumentException e) {
            return BotResponse.text("该菜单按钮已失效，请重新发送“菜单”。");
        }
        return helpMenuService.categoryHelp(interaction.getGroupOpenId(), group);
    }

    private String callbackData(InteractionDataDto data) {
        JsonNode resolved = data == null ? null : data.getResolved();
        if (resolved == null || !resolved.isObject()) {
            return "";
        }
        return resolved.path("button_data").asText("");
    }
}
