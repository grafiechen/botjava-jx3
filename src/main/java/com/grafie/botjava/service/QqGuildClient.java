package com.grafie.botjava.service;

import com.grafie.botjava.entity.dto.qq.QqChannelDto;
import com.grafie.botjava.entity.dto.qq.QqChannelRequestDto;
import com.grafie.botjava.entity.dto.qq.QqGuildInfoDto;
import com.grafie.botjava.qq.QqOpenApiClient;
import com.grafie.botjava.util.ObjectMapperUtil;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@SuppressWarnings("unchecked")
public class QqGuildClient {

    private static final String CURRENT_BOT_GUILDS_PATH = "/users/@me/guilds";
    private static final String GUILD_PATH = "/guilds/%s";
    private static final String GUILD_CHANNELS_PATH = "/guilds/%s/channels";
    private static final String CHANNEL_PATH = "/channels/%s";

    private final QqOpenApiClient openApiClient;

    public QqGuildClient(QqOpenApiClient openApiClient) {
        this.openApiClient = openApiClient;
    }

    public List<QqGuildInfoDto> listCurrentBotGuilds(String before, String after, Integer limit) {
        QqGuildInfoDto[] result = openApiClient.get(CURRENT_BOT_GUILDS_PATH,
                pagingQuery(before, after, limit), QqGuildInfoDto[].class);
        return result == null ? List.of() : Arrays.asList(result);
    }

    public QqGuildInfoDto getGuild(String guildId) {
        return openApiClient.get(String.format(GUILD_PATH, safePathSegment(guildId, "QQ guild ID")),
                Map.of(), QqGuildInfoDto.class);
    }

    public List<QqChannelDto> listChannels(String guildId) {
        QqChannelDto[] result = openApiClient.get(
                String.format(GUILD_CHANNELS_PATH, safePathSegment(guildId, "QQ guild ID")),
                Map.of(), QqChannelDto[].class);
        return result == null ? List.of() : Arrays.asList(result);
    }

    public QqChannelDto createChannel(String guildId, QqChannelRequestDto request) {
        return openApiClient.post(
                String.format(GUILD_CHANNELS_PATH, safePathSegment(guildId, "QQ guild ID")),
                requestBody(request, "QQ channel create request must not be null"),
                QqChannelDto.class);
    }

    public QqChannelDto getChannel(String channelId) {
        return openApiClient.get(String.format(CHANNEL_PATH, safePathSegment(channelId, "QQ channel ID")),
                Map.of(), QqChannelDto.class);
    }

    public QqChannelDto updateChannel(String channelId, QqChannelRequestDto request) {
        return openApiClient.patch(
                String.format(CHANNEL_PATH, safePathSegment(channelId, "QQ channel ID")),
                requestBody(request, "QQ channel update request must not be null"),
                QqChannelDto.class);
    }

    public void deleteChannel(String channelId) {
        openApiClient.delete(String.format(CHANNEL_PATH, safePathSegment(channelId, "QQ channel ID")),
                Map.of(), String.class);
    }

    private Map<String, Object> pagingQuery(String before, String after, Integer limit) {
        Map<String, Object> query = new LinkedHashMap<>();
        if (before != null && !before.isBlank()) {
            query.put("before", before.trim());
        }
        if (after != null && !after.isBlank()) {
            query.put("after", after.trim());
        }
        if (limit != null) {
            query.put("limit", limit);
        }
        return query;
    }

    private Map<String, Object> requestBody(Object request, String message) {
        if (request == null) {
            throw new IllegalArgumentException(message);
        }
        return ObjectMapperUtil.getObjectMapper().convertValue(request, Map.class);
    }

    private String safePathSegment(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        String trimmed = value.trim();
        if (trimmed.contains("/")) {
            throw new IllegalArgumentException(name + " must not contain path separator");
        }
        return trimmed;
    }
}