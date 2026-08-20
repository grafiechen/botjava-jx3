package com.grafie.botjava.jx3.http.data.official;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 官方未声明响应 schema 的类型化动态对象边界。
 */
public class FlexibleOfficialData {
    private final Map<String, JsonNode> fields = new LinkedHashMap<>();

    @JsonAnySetter
    public void put(String name, JsonNode value) {
        fields.put(name, value);
    }

    @JsonAnyGetter
    public Map<String, JsonNode> fields() {
        return fields;
    }

    public JsonNode get(String name) {
        return fields.get(name);
    }

    public String text(String name) {
        JsonNode value = fields.get(name);
        return value == null || value.isNull() ? null : value.asText();
    }
}