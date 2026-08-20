package com.grafie.botjava.entity.dto.qq;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QqJoinApprovalStrategyRequestDto {

    @JsonProperty("group_openids")
    private List<String> groupOpenids;

    @JsonProperty("group_ids")
    private List<Long> groupIds;

    @JsonProperty("is_enable")
    private String isEnable;

    @JsonProperty("expire_at")
    private String expireAt;

    private String remark;
}