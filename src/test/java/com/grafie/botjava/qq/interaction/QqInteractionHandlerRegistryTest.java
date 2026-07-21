package com.grafie.botjava.qq.interaction;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.interaction.InteractionCreateDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QqInteractionHandlerRegistryTest {

    @Test
    void shouldDispatchToOnlyMatchingHandler() {
        InteractionCreateDto interaction = new InteractionCreateDto();
        QqInteractionHandler ignored = mock(QqInteractionHandler.class);
        QqInteractionHandler selected = mock(QqInteractionHandler.class);
        when(selected.supports(interaction)).thenReturn(true);
        when(selected.handle(interaction)).thenReturn(BotResponse.text("ok"));

        BotResponse response = new QqInteractionHandlerRegistry(List.of(ignored, selected))
                .dispatch(interaction);

        assertEquals("ok", response.getContent());
    }

    @Test
    void shouldReturnNullWhenNoHandlerSupportsInteraction() {
        assertNull(new QqInteractionHandlerRegistry(List.of()).dispatch(new InteractionCreateDto()));
    }

    @Test
    void shouldRejectAmbiguousInteractionHandlers() {
        InteractionCreateDto interaction = new InteractionCreateDto();
        QqInteractionHandler first = mock(QqInteractionHandler.class);
        QqInteractionHandler second = mock(QqInteractionHandler.class);
        when(first.supports(interaction)).thenReturn(true);
        when(second.supports(interaction)).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> new QqInteractionHandlerRegistry(List.of(first, second)).dispatch(interaction));
    }
}
