package com.grafie.botjava.jx3.http.data.saohua;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 撩人骚话
 *
 * @author Grafie
 * @since 1.0.0
 */
@Data
public class SaohuaRandomData {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("text")
    private String text;
}
