package com.grafie.botjava.service;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.http.util.REGEX;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HelpMenuServiceTest {

    @Test
    void shouldBuildCategoryFromAvailableCommandDefinitions() {
        GroupCommandPolicy policy = mock(GroupCommandPolicy.class);
        when(policy.availableDefinitions("group-1"))
                .thenReturn(List.of(REGEX.Help, REGEX.ServerCheck, REGEX.TradeRecord));
        HelpMenuService service = new HelpMenuService(policy);

        BotResponse response = service.categoryHelp("group-1", REGEX.CommandGroup.FREE);

        assertEquals(BotResponse.ResponseType.TEXT, response.getResponseType());
        assertTrue(response.getContent().contains("开服状态"));
        assertTrue(!response.getContent().contains("物品价格"));
    }

    @Test
    void shouldBuildFourSafeCallbackButtons() {
        BotResponse response = new HelpMenuService(mock(GroupCommandPolicy.class)).interactiveMenu();

        assertEquals(BotResponse.ResponseType.MARKDOWN, response.getResponseType());
        assertEquals(2, response.getKeyboard().getContent().getRows().size());
        response.getKeyboard().getContent().getRows().stream()
                .flatMap(row -> row.getButtons().stream())
                .forEach(button -> {
                    assertEquals(1, button.getAction().getType());
                    assertEquals(2, button.getAction().getPermission().getType());
                    assertTrue(button.getAction().getData().startsWith(HelpMenuService.CALLBACK_PREFIX));
                });
    }
}
