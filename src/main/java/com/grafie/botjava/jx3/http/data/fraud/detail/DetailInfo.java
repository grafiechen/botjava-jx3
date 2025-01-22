package com.grafie.botjava.jx3.http.data.fraud.detail;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.grafie.botjava.util.TimeUtils;
import lombok.Data;

@Data
public class DetailInfo {
    @JsonProperty("title")
    private String title;
    @JsonProperty("tid")
    private Long tid;
    @JsonProperty("text")
    private String text;
    @JsonProperty("time")
    private String time;

    public void setTime(Long time) {
        this.time = TimeUtils.timeFormatting(time);
    }
}
