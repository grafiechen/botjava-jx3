package com.grafie.botjava.jx3.http.data.server;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.grafie.botjava.util.TimeUtils;
import lombok.Data;

/**
 * 阵营事件
 *
 * @author Grafie
 * @since 1.0.0
 */
@Data
public class ServerEventData {
    @JsonProperty("id")
    private int id;

    @JsonProperty("camp_name")
    private String campName;

    @JsonProperty("fenxian_name")
    @JsonAlias("fenxian_server_name")
    private String fenxianName;

    @JsonProperty("friend_name")
    @JsonAlias("friend_server_name")
    private String friendName;

    @JsonProperty("role_name")
    private String roleName;

    @JsonProperty("seize_time")
    @JsonAlias("set_time")
    private String seizeTime;

    public void setSeizeTime(long seizeTime) {
        this.seizeTime = TimeUtils.timeFormatting(seizeTime);
    }
}
