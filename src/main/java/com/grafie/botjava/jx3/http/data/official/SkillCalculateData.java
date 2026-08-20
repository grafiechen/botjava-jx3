package com.grafie.botjava.jx3.http.data.official;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 急速计算档位。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SkillCalculateData {
    private Double duration;
    private Double hastePercent;
    private Double hastePercentLimit;
    private Integer nowFrame;
    private Integer uNowFrame;
    private Integer surplusNowFrame;
    private Integer level;
}