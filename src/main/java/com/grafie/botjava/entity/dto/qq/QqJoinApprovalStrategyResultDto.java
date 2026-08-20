package com.grafie.botjava.entity.dto.qq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QqJoinApprovalStrategyResultDto {

    @JsonProperty("strategy_id")
    private String strategyId;

    @JsonProperty("is_enable")
    private String isEnable;

    @JsonProperty("expire_at")
    private String expireAt;
}