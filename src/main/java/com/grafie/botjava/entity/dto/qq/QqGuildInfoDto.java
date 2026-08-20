package com.grafie.botjava.entity.dto.qq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QqGuildInfoDto {

    private String id;
    private String name;
    private String icon;

    @JsonProperty("owner_id")
    private String ownerId;

    private Boolean owner;

    @JsonProperty("joined_at")
    private String joinedAt;

    @JsonProperty("member_count")
    private Integer memberCount;

    @JsonProperty("max_members")
    private Integer maxMembers;

    private String description;
}