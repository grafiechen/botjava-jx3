package com.grafie.botjava.entity.dto.qq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * QQ OpenAPI 当前机器人身份。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QqBotUserDto {

    private String id;
    private String username;
    private String avatar;
    private Boolean bot;

    @JsonProperty("union_openid")
    private String unionOpenId;

    @JsonProperty("union_user_account")
    private String unionUserAccount;
}
