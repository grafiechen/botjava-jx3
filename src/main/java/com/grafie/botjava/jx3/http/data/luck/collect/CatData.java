package com.grafie.botjava.jx3.http.data.luck.collect;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.grafie.botjava.util.TimeUtils;
import lombok.Data;

@Data
public class CatData {
    @JsonProperty("name")
    private String name;

    @JsonProperty("time")
    public String time;

    public void setTime(Long time) {
        this.time = TimeUtils.timeFormatting(time);
    }
}
