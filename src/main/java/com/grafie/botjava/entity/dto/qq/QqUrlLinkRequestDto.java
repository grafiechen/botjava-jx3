package com.grafie.botjava.entity.dto.qq;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QqUrlLinkRequestDto {

    @JsonProperty("url_link")
    private String urlLink;

    @JsonProperty("callback_data")
    private String callbackData;
}