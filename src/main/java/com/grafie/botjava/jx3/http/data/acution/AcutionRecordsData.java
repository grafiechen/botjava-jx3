package com.grafie.botjava.jx3.http.data.acution;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.grafie.botjava.util.TimeUtils;
import lombok.Data;

/**
 * 拍卖记录
 *
 * @author Grafie
 * @since 2024/9/25  14:17
 */
@Data
public class AcutionRecordsData {
    @JsonProperty("id")
    private Long id;
    @JsonProperty("zone")
    private String zone;
    @JsonProperty("server")
    private String server;
    @JsonProperty("role_name")
    private String roleName;
    @JsonProperty("camp_name")
    private String campName;
    @JsonProperty("map_name")
    private String mapName;
    @JsonProperty("item_name")
    @JsonAlias("name")
    private String itemName;
    @JsonProperty("item_amount")
    @JsonAlias("amount")
    private String itemAmount;
    @JsonProperty("time")
    private String time;

    public void setTime(Long time) {
        this.time = TimeUtils.timeFormatting(time);
    }
}
