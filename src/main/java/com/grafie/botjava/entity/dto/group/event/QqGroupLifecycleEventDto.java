package com.grafie.botjava.entity.dto.group.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * QQ 机器人群生命周期与主动消息授权事件的稳定字段。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QqGroupLifecycleEventDto {

    private String timestamp;

    @JsonProperty("group_openid")
    private String groupOpenId;

    @JsonProperty("op_member_openid")
    private String operatorMemberOpenId;
}
