package com.grafie.botjava.jx3.http.data.valuables.collect;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 掉落汇总
 *
 * @author Grafie
 * @since 1.0.0
 */
@Data
public class ValuablesCollectData {
    @JsonProperty("name")
    private String name;

    @JsonProperty("count")
    private Integer count;

    @JsonProperty("data")
    private DropData data;
}

