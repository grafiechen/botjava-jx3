package com.grafie.botjava.entity.dto.interaction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * QQ 互动事件。resolved 的结构由 data.type 决定，由具体业务处理器解释。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InteractionCreateDto {
    private String id;
    @JsonProperty("application_id")
    private String applicationId;
    private Integer type;
    private InteractionDataDto data;
    @JsonProperty("guild_id")
    private String guildId;
    @JsonProperty("channel_id")
    private String channelId;
    private Integer version;
    @JsonProperty("group_openid")
    private String groupOpenId;
    @JsonProperty("chat_type")
    private Integer chatType;
    private String scene;
    @JsonProperty("user_openid")
    private String userOpenId;
    private String timestamp;
    @JsonProperty("group_member_openid")
    private String groupMemberOpenId;
}
