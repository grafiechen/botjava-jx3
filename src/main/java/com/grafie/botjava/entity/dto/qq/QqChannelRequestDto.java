package com.grafie.botjava.entity.dto.qq;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QqChannelRequestDto {

    private String name;
    private Integer type;

    @JsonProperty("sub_type")
    private Integer subType;

    private Integer position;

    @JsonProperty("parent_id")
    private String parentId;

    @JsonProperty("private_type")
    private Integer privateType;

    @JsonProperty("private_user_ids")
    private List<String> privateUserIds;

    @JsonProperty("speak_permission")
    private Integer speakPermission;

    @JsonProperty("application_id")
    private String applicationId;
}