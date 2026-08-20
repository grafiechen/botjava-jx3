package com.grafie.botjava.entity.dto.qq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QqGroupRestrictChatSettingDto {

    @JsonProperty("global_rule")
    private GlobalMuteRule globalRule;

    private List<MemberMuteState> members;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GlobalMuteRule {
        private String mode;

        @JsonProperty("schedule_rules")
        private List<MuteScheduleRule> scheduleRules;

        @JsonProperty("recurring_rules")
        private List<MuteRecurringRule> recurringRules;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MuteScheduleRule {
        @JsonProperty("task_id")
        private String taskId;

        @JsonProperty("start_at")
        private String startAt;

        @JsonProperty("end_at")
        private String endAt;

        private Boolean enabled;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MuteRecurringRule {
        @JsonProperty("task_id")
        private String taskId;

        private List<Integer> weekdays;

        @JsonProperty("start_time")
        private String startTime;

        @JsonProperty("end_time")
        private String endTime;

        private Boolean enabled;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MemberMuteState {
        @JsonProperty("member_openid")
        private String memberOpenid;

        @JsonProperty("mute_expire_at")
        private String muteExpireAt;

        private String username;

        @JsonProperty("union_openid")
        private String unionOpenid;
    }
}