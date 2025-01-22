package com.grafie.botjava.entity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author grafie.chen
 * @since 2025/1/22  12:36
 */
@Data
public class AuthorDto {
    private String id;
    @JsonProperty(value = "member_openid")
    private String memberOpenid;
    @JsonProperty(value = "union_openid")
    private String unionOpenid;
}
