package com.grafie.botjava.entity.dto.interaction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * QQ 互动数据。type 常见值：9 搜索、11 消息按钮、12 C2C 菜单。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InteractionDataDto {
    private String name;
    private Integer type;
    private JsonNode resolved;
}
