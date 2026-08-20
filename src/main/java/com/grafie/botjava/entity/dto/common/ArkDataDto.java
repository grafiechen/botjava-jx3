package com.grafie.botjava.entity.dto.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * QQ 接收侧结构化卡片消息数据。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ArkDataDto {

    private String prompt;

    @JsonProperty("ark_type")
    private String arkType;

    @JsonProperty("ark_name")
    private String arkName;

    private Map<String, Object> fields;
}
