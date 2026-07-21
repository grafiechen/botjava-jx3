package com.grafie.botjava.jx3.http.data.role;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.grafie.botjava.util.TimeUtils;
import lombok.Data;

/**
 * 角色名片
 *
 * @author Grafie
 * @since 2024/9/25  14:24
 */
@Data
public class RoleShowCardData {
    @JsonProperty("zoneName")
    @JsonAlias("zone")
    private String zone;
    @JsonProperty("serverName")
    @JsonAlias("server")
    private String server;
    @JsonProperty("showHash")
    @JsonAlias("global")
    private String global;
    @JsonProperty("roleName")
    @JsonAlias("name")
    private String name;
    @JsonProperty("showAvatar")
    @JsonAlias({"static", "avatar"})
    private String staticUrl;
    @JsonProperty("showIndex")
    private Integer showIndex;
    @JsonProperty("showActive")
    private Boolean showActive;
    @JsonProperty("cache")
    private String cache;


    public void setCache(Long cache) {
        this.cache = TimeUtils.timeFormatting(cache);
    }

    public void setCacheTime(Long cacheTime) {
        this.cache = TimeUtils.timeFormatting(cacheTime);
    }

    public void setSaveTime(Long saveTime) {
        this.cache = TimeUtils.timeFormatting(saveTime);
    }
}
