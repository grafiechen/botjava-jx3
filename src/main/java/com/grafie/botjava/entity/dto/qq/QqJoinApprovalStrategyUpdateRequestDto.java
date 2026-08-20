package com.grafie.botjava.entity.dto.qq;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QqJoinApprovalStrategyUpdateRequestDto {

    @JsonProperty("is_enable")
    private String isEnable;

    @JsonProperty("expire_at")
    private String expireAt;

    @JsonProperty("group_action")
    private GroupAction groupAction;

    private String remark;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class GroupAction {
        public static final String OP_ADD = "add";
        public static final String OP_DELETE = "del";

        private String op;

        @JsonProperty("group_openids")
        private List<String> groupOpenids;

        @JsonProperty("group_ids")
        private List<Long> groupIds;
    }
}