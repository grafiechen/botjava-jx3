package com.grafie.botjava.jx3.http.data.role;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author Grafie
 * @since 2024/9/25  14:30
 */
@Data
public class RoleShowRandomData {
    @JsonProperty("zoneName")
    private String zone;
    @JsonProperty("serverName")
    @JsonAlias("server")
    private String server;
    @JsonProperty("roleName")
    @JsonAlias("name")
    private String name;
    @JsonProperty("showHash")
    private String showHash;
    @JsonProperty("showIndex")
    private Integer showIndex;
    @JsonProperty("showAvatar")
    @JsonAlias("avatar")
    private String avatar;
    @JsonProperty("status")
    private Integer status;
}
