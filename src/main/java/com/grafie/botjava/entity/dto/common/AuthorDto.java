package com.grafie.botjava.entity.dto.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author grafie.chen
 * @since 2025/1/22  12:36
 */
@Data
public class AuthorDto {
    private String id;
    private String username;
    private Boolean bot;
    @JsonProperty(value = "member_openid")
    private String memberOpenid;
    @JsonProperty(value = "member_role")
    private String memberRole;
    @JsonProperty(value = "union_openid")
    private String unionOpenid;
}
