package com.grafie.botjava.jx3.http.data.school.matirx;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DescriptiveSkill {
    @JsonProperty("desc")
    private String description;

    @JsonProperty("level")
    private Integer level;

    @JsonProperty("name")
    private String name;
}
