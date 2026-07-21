package com.grafie.botjava.jx3.http.data.school.matirx;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 阵法效果
 *
 * @author Grafie
 * @since 1.0.0
 */
@Data
public class SchoolMatrixData {
    @JsonProperty("name")
    private String name;

    @JsonProperty("skillName")
    private String skillName;

    @JsonProperty("data")
    @JsonAlias("descs")
    private List<DescriptiveSkill> effects;
}

