package com.grafie.botjava.entity.dto.qq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QqGroupJoinRequestListDto {

    private List<JoinRequest> list;

    @JsonProperty("next_cursor")
    private String nextCursor;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JoinRequest {
        @JsonProperty("join_request_id")
        private String joinRequestId;

        @JsonProperty("risk_tips")
        private String riskTips;

        @JsonProperty("union_openid")
        private String unionOpenid;

        @JsonProperty("member_openid")
        private String memberOpenid;

        private String username;

        @JsonProperty("apply_at")
        private String applyAt;

        @JsonProperty("apply_source")
        private String applySource;

        @JsonProperty("invited_by")
        private String invitedBy;

        private Boolean bot;

        @JsonProperty("verify_info")
        private VerifyInfo verifyInfo;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VerifyInfo {
        private String method;

        @JsonProperty("verify_message")
        private String verifyMessage;

        @JsonProperty("review_qa_list")
        private List<ReviewQa> reviewQaList;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ReviewQa {
        private String question;
        private String answer;
    }
}