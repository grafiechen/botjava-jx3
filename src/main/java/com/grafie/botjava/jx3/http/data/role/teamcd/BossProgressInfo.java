package com.grafie.botjava.jx3.http.data.role.teamcd;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BossProgressInfo {
    @JsonProperty("finished")
    private Boolean finished;
    @JsonProperty("icon")
    private String icon;
    @JsonProperty("index")
    private String index;
    @JsonProperty("name")
    private String name;
    @JsonProperty("progressId")
    private String progressId;

}
