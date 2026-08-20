package com.grafie.botjava.jx3.ws.data;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 官方已定义但项目尚未类型化的 WS 事件兜底数据。
 */
public class GenericWsData extends BaseWsData {
    private final Map<String, Object> fields = new LinkedHashMap<>();

    @JsonAnySetter
    public void putField(String name, Object value) {
        fields.put(name, value);
    }

    @JsonAnyGetter
    public Map<String, Object> jsonFields() {
        return fields;
    }

    @JsonIgnore
    public Map<String, Object> getFields() {
        return fields;
    }
}