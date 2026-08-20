package com.grafie.botjava.entity.dto.qq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QqJoinApprovalStrategyListDto {

    private List<JoinApprovalStrategy> strategies;

    @JsonProperty("next_cursor")
    private String nextCursor;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JoinApprovalStrategy {
        @JsonProperty("strategy_id")
        private String strategyId;

        @JsonProperty("group_openids")
        private List<String> groupOpenids;

        @JsonProperty("group_ids")
        private List<Long> groupIds;

        @JsonProperty("whitelist_user_count")
        private Integer whitelistUserCount;

        @JsonProperty("is_enable")
        private String isEnable;

        @JsonProperty("expire_at")
        private String expireAt;

        @JsonProperty("created_at")
        private String createdAt;

        @JsonProperty("updated_at")
        private String updatedAt;

        private String remark;
    }
}