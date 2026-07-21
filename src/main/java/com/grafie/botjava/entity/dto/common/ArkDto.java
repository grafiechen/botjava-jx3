package com.grafie.botjava.entity.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * QQ Ark 模板消息结构。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArkDto {
    @JsonProperty("template_id")
    private Integer templateId;
    private List<KeyValue> kv;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class KeyValue {
        private String key;
        private String value;
        private List<Obj> obj;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Obj {
        @JsonProperty("obj_kv")
        private List<ObjKeyValue> objKv;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ObjKeyValue {
        private String key;
        private String value;
    }
}
