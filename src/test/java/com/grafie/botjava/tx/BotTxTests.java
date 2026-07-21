package com.grafie.botjava.tx;

import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.entity.dto.interaction.InteractionCreateDto;
import com.grafie.botjava.entity.dto.payload.Payload;
import com.grafie.botjava.util.ObjectMapperUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BotTxTests {

    @Test
    void groupMessagePayloadTest() throws IOException {
        String message = Files.readString(
                Path.of("docs", "testing", "payloads", "group-message-create.json"),
                StandardCharsets.UTF_8
        );
        Payload payload = ObjectMapperUtil.readValue(message, Payload.class);
        GroupAtMessageCreateDto messageDto = ObjectMapperUtil.readValue(payload.getD(), GroupAtMessageCreateDto.class);

        assertEquals("GROUP_MESSAGE_CREATE", payload.getT());
        assertEquals("/开服 乾坤一掷", messageDto.getContent());
        assertEquals("LOCAL_GROUP_001", messageDto.getGroupId());
        assertEquals("LOCAL_GROUP_001", messageDto.getGroupOpenid());
        assertEquals(0, messageDto.getMessageType());
        assertNotNull(messageDto.getTimestamp());
    }

    @Test
    void interactionPayloadTest() throws IOException {
        String message = Files.readString(
                Path.of("docs", "testing", "payloads", "interaction-create.json"),
                StandardCharsets.UTF_8
        );
        Payload payload = ObjectMapperUtil.readValue(message, Payload.class);
        InteractionCreateDto interaction = ObjectMapperUtil.readValue(
                payload.getD(), InteractionCreateDto.class);

        assertEquals("INTERACTION_CREATE", payload.getT());
        assertEquals("LOCAL_INTERACTION_001", interaction.getId());
        assertEquals("LOCAL_GROUP_001", interaction.getGroupOpenId());
        assertEquals("LOCAL_ACCOUNT_001", interaction.getGroupMemberOpenId());
        assertEquals(11, interaction.getData().getType());
        assertEquals("jx3:help:BASIC", interaction.getData().getResolved().path("button_data").asText());
    }
}
