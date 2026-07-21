package com.grafie.botjava.qq.interaction;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.interaction.InteractionCreateDto;
import com.grafie.botjava.entity.dto.interaction.InteractionDataDto;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.HelpMenuService;
import com.grafie.botjava.util.ObjectMapperUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HelpInteractionHandlerTest {

    @Test
    void shouldHandleOnlyKnownHelpCallbackPrefix() throws Exception {
        HelpMenuService menuService = mock(HelpMenuService.class);
        when(menuService.categoryHelp("group-1", REGEX.CommandGroup.MEMBER))
                .thenReturn(BotResponse.text("会员查询"));
        HelpInteractionHandler handler = new HelpInteractionHandler(menuService);
        InteractionCreateDto interaction = interaction(11, "jx3:help:MEMBER");

        assertTrue(handler.supports(interaction));
        handler.handle(interaction);

        verify(menuService).categoryHelp("group-1", REGEX.CommandGroup.MEMBER);
        assertFalse(handler.supports(interaction(11, "开服 乾坤一掷")));
        assertFalse(handler.supports(interaction(9, "jx3:help:MEMBER")));
    }

    @Test
    void shouldReturnExpiredMessageForUnknownCategory() throws Exception {
        HelpInteractionHandler handler = new HelpInteractionHandler(mock(HelpMenuService.class));

        BotResponse response = handler.handle(interaction(11, "jx3:help:REMOVED"));

        assertTrue(response.getContent().contains("已失效"));
    }

    private static InteractionCreateDto interaction(int type, String buttonData) throws Exception {
        InteractionDataDto data = new InteractionDataDto();
        data.setType(type);
        data.setResolved(ObjectMapperUtil.getObjectMapper().readTree(
                "{\"button_data\":\"" + buttonData + "\"}"));
        InteractionCreateDto interaction = new InteractionCreateDto();
        interaction.setGroupOpenId("group-1");
        interaction.setData(data);
        return interaction;
    }
}
