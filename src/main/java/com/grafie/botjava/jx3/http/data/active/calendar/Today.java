package com.grafie.botjava.jx3.http.data.active.calendar;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Today {
    @JsonProperty("date")
    private String date;
    @JsonProperty("week")
    private String week;
    @JsonProperty("year")
    private String year;
    @JsonProperty("month")
    private String month;
    @JsonProperty("day")
    private String day;
}
