package com.grafie.botjava.entity.dto.qq;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QqJoinApprovalStrategyWhitelistRequestDto {

    public static final String OP_ADD = "add";
    public static final String OP_DELETE = "del";

    private String op;

    @JsonProperty("whitelist_users")
    private List<String> whitelistUsers;
}