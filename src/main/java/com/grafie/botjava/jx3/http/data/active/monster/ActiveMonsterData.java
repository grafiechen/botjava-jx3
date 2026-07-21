package com.grafie.botjava.jx3.http.data.active.monster;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.grafie.botjava.util.TimeUtils;
import lombok.Data;

import java.util.List;

/**
 * 百战首领
 *
 * @author Grafie
 * @since 1.0.0
 */
@Data
public class ActiveMonsterData {
    @JsonProperty("week")
    private String week;
    @JsonProperty("boss")
    private String boss;
    @JsonProperty("start")
    private String start;
    @JsonProperty("end")
    private String end;
    @JsonProperty("list")
    @JsonAlias("data")
    private List<MonsterInfo> list;

    public void setStart(Long start) {
        this.start = TimeUtils.timeFormatting(start);
    }

    public void setEnd(Long end) {
        this.end = TimeUtils.timeFormatting(end);
    }
}

