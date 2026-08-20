package com.grafie.botjava.entity.dto.qq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QqJoinApprovalStrategyWhitelistResultDto {

    @JsonProperty("strategy_id")
    private String strategyId;

    @JsonProperty("whitelist_user_count")
    private Integer whitelistUserCount;

    @JsonProperty("updated_at")
    private String updatedAt;
}