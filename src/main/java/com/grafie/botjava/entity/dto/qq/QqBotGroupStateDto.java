package com.grafie.botjava.entity.dto.qq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * QQ 机器人在群内的状态信息。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QqBotGroupStateDto {

    @JsonProperty("member_openid")
    private String memberOpenid;

    @JsonProperty("joined_at")
    private String joinedAt;

    @JsonProperty("allow_proactive_msg")
    private Boolean allowProactiveMsg;

    @JsonProperty("recv_msg_setting")
    private String recvMsgSetting;

    @JsonProperty("member_role")
    private String memberRole;
}
