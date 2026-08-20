package com.grafie.botjava.entity.dto.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class InputNotifyDto {

    @JsonProperty("input_type")
    private Integer inputType;

    @JsonProperty("input_second")
    private Integer inputSecond;
}