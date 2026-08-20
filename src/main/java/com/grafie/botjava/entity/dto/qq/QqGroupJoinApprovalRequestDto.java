package com.grafie.botjava.entity.dto.qq;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QqGroupJoinApprovalRequestDto {

    public static final String OP_APPROVE = "approve";
    public static final String OP_DECLINE = "decline";

    private String op;

    @JsonProperty("join_request_id")
    private String joinRequestId;

    @JsonProperty("reject_reason")
    private String rejectReason;

    @JsonProperty("add_to_member_blacklist")
    private Boolean addToMemberBlacklist;

    public static QqGroupJoinApprovalRequestDto approve(String joinRequestId) {
        QqGroupJoinApprovalRequestDto request = new QqGroupJoinApprovalRequestDto();
        request.setOp(OP_APPROVE);
        request.setJoinRequestId(joinRequestId);
        return request;
    }

    public static QqGroupJoinApprovalRequestDto decline(String joinRequestId, String rejectReason,
                                                        Boolean addToMemberBlacklist) {
        QqGroupJoinApprovalRequestDto request = new QqGroupJoinApprovalRequestDto();
        request.setOp(OP_DECLINE);
        request.setJoinRequestId(joinRequestId);
        request.setRejectReason(rejectReason);
        request.setAddToMemberBlacklist(addToMemberBlacklist);
        return request;
    }
}