package com.grafie.botjava.service;

import com.grafie.botjava.entity.dto.qq.QqChannelDto;
import com.grafie.botjava.entity.dto.qq.QqChannelRequestDto;
import com.grafie.botjava.entity.dto.qq.QqGuildInfoDto;
import com.grafie.botjava.qq.QqOpenApiClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QqGuildClientTest {

    @Test
    void shouldUseOfficialGuildAndChannelPaths() {
        QqOpenApiClient openApiClient = mock(QqOpenApiClient.class);
        QqGuildInfoDto guild = new QqGuildInfoDto();
        guild.setId("guild-1");
        QqChannelDto channel = new QqChannelDto();
        channel.setId("channel-1");
        when(openApiClient.get(eq("/users/@me/guilds"), anyMap(), eq(QqGuildInfoDto[].class)))
                .thenReturn(new QqGuildInfoDto[]{guild});
        when(openApiClient.get(eq("/guilds/guild-1"), eq(Map.of()), eq(QqGuildInfoDto.class))).thenReturn(guild);
        when(openApiClient.get(eq("/guilds/guild-1/channels"), eq(Map.of()), eq(QqChannelDto[].class)))
                .thenReturn(new QqChannelDto[]{channel});
        when(openApiClient.get(eq("/channels/channel-1"), eq(Map.of()), eq(QqChannelDto.class))).thenReturn(channel);
        when(openApiClient.post(eq("/guilds/guild-1/channels"), anyMap(), eq(QqChannelDto.class))).thenReturn(channel);
        when(openApiClient.patch(eq("/channels/channel-1"), anyMap(), eq(QqChannelDto.class))).thenReturn(channel);
        QqGuildClient client = new QqGuildClient(openApiClient);
        QqChannelRequestDto request = new QqChannelRequestDto();
        request.setName("notice");
        request.setType(0);

        assertEquals(1, client.listCurrentBotGuilds(" before-1 ", "after-1", 20).size());
        assertEquals("guild-1", client.getGuild("guild-1").getId());
        assertEquals("channel-1", client.listChannels("guild-1").get(0).getId());
        assertEquals("channel-1", client.createChannel("guild-1", request).getId());
        assertEquals("channel-1", client.getChannel("channel-1").getId());
        assertEquals("channel-1", client.updateChannel("channel-1", request).getId());
        client.deleteChannel("channel-1");

        ArgumentCaptor<Map<String, Object>> queryCaptor = ArgumentCaptor.forClass(Map.class);
        verify(openApiClient).get(eq("/users/@me/guilds"), queryCaptor.capture(), eq(QqGuildInfoDto[].class));
        assertEquals("before-1", queryCaptor.getValue().get("before"));
        assertEquals("after-1", queryCaptor.getValue().get("after"));
        assertEquals(20, queryCaptor.getValue().get("limit"));
        ArgumentCaptor<Map<String, Object>> bodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(openApiClient).post(eq("/guilds/guild-1/channels"), bodyCaptor.capture(), eq(QqChannelDto.class));
        assertEquals("notice", bodyCaptor.getValue().get("name"));
        assertEquals(0, bodyCaptor.getValue().get("type"));
        verify(openApiClient).delete(eq("/channels/channel-1"), eq(Map.of()), eq(String.class));
    }
}