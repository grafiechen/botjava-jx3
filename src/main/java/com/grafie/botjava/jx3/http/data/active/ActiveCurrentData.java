package com.grafie.botjava.jx3.http.data.active;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 活动日历返回值
 *
 * @author Grafie
 * @since 1.0.0
 */
@Data
public class ActiveCurrentData {

    @JsonProperty("date")
    private String date;

    @JsonProperty("week")
    private String week;

    @JsonProperty("war")
    private String war;

    @JsonProperty("battle")
    private String battle;

    @JsonProperty("orecar")
    private String orecar;

    @JsonProperty("school")
    private String school;

    @JsonProperty("rescue")
    private String rescue;

    @JsonProperty("draw")
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> draw;

    @JsonProperty("leader")
    private List<String> leader;

    @JsonProperty("team")
    private List<String> team;

    @JsonProperty("lucky")
    @JsonAlias("luck")
    private List<String> luck;

    @JsonProperty("card")
    private List<String> card;

    @JsonProperty("weekly")
    private Weekly weekly;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Weekly {

        @JsonProperty("conn")
        private List<String> conn;

        @JsonProperty("raid")
        private List<String> raid;
    }

}
