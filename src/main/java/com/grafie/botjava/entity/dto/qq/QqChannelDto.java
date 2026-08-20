package com.grafie.botjava.entity.dto.qq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QqChannelDto {

    private String id;

    @JsonProperty("guild_id")
    private String guildId;

    private String name;
    private Integer type;

    @JsonProperty("sub_type")
    private Integer subType;

    private Integer position;

    @JsonProperty("parent_id")
    private String parentId;

    @JsonProperty("owner_id")
    private String ownerId;

    @JsonProperty("private_type")
    private Integer privateType;

    @JsonProperty("speak_permission")
    private Integer speakPermission;

    @JsonProperty("application_id")
    private String applicationId;

    private String permissions;
}