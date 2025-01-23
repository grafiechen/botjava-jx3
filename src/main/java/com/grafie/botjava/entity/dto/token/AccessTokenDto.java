package com.grafie.botjava.entity.dto.token;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 登录token获取值
 * @author grafie.chen
 * @since 2025/1/23  13:50
 */
@Data
public class AccessTokenDto {
    @JsonProperty(value = "access_token")
    private String accessToken;
    @JsonProperty(value = "expires_in")
    private Integer expiresIn;
}
