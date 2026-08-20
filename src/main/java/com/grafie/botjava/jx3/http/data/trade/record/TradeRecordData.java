package com.grafie.botjava.jx3.http.data.trade.record;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.grafie.botjava.util.ObjectMapperUtil;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 物品价格
 *
 * @author Grafie
 * @since 1.0.0
 */
@Data
public class TradeRecordData {
    @JsonProperty("id")
    private Integer id;

    @JsonProperty("class")
    private String classType;

    @JsonProperty("category")
    private String category;

    @JsonProperty("subclass")
    private String subclass;

    @JsonProperty("name")
    private String name;

    @JsonProperty("alias")
    private String alias;

    @JsonProperty("subalias")
    private String subalias;

    @JsonProperty("raw")
    private String raw;

    @JsonProperty("retail")
    private Integer retail;

    @JsonProperty("value")
    private String value;

    @JsonProperty("level")
    private Integer level;

    @JsonProperty("desc")
    private String desc;

    @JsonProperty("view")
    private String view;

    @JsonProperty("date")
    private String date;

    private List<SaleData> data = new ArrayList<>();

    private List<PriceGroup> groups = new ArrayList<>();

    @JsonAlias("data")
    @JsonProperty("list")
    public void setList(JsonNode node) {
        ParseResult result = parseList(node);
        this.data = result.records();
        this.groups = result.groups();
    }

    public void setData(List<SaleData> data) {
        this.data = data == null ? new ArrayList<>() : data;
    }

    private ParseResult parseList(JsonNode node) {
        List<PriceGroup> parsedGroups = new ArrayList<>();
        List<SaleData> records = new ArrayList<>();
        appendNode(parsedGroups, records, node, null);
        return new ParseResult(records, parsedGroups);
    }

    private void appendNode(List<PriceGroup> parsedGroups, List<SaleData> records, JsonNode node, String fallbackName) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            int unnamedIndex = 1;
            for (JsonNode item : node) {
                String itemFallbackName = fallbackName;
                if (fallbackName == null && item.isArray()) {
                    itemFallbackName = "记录 " + unnamedIndex++;
                }
                appendNode(parsedGroups, records, item, itemFallbackName);
            }
            return;
        }
        if (!node.isObject()) {
            return;
        }
        if (node.has("list") && node.get("list").isArray()) {
            String groupName = text(node.get("name"));
            if (groupName == null) {
                groupName = fallbackName;
            }
            List<SaleData> groupRecords = parseRecords(node.get("list"));
            parsedGroups.add(new PriceGroup(groupName, groupRecords));
            records.addAll(groupRecords);
            return;
        }
        SaleData record = toSaleData(node);
        if (record != null) {
            records.add(record);
            if (fallbackName != null) {
                parsedGroups.add(new PriceGroup(fallbackName, List.of(record)));
            }
        }
    }

    private List<SaleData> parseRecords(JsonNode node) {
        List<SaleData> records = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return records;
        }
        for (JsonNode item : node) {
            if (item.isArray()) {
                records.addAll(parseRecords(item));
                continue;
            }
            SaleData record = toSaleData(item);
            if (record != null) {
                records.add(record);
            }
        }
        return records;
    }

    private SaleData toSaleData(JsonNode node) {
        if (node == null || !node.isObject()) {
            return null;
        }
        if (!node.has("value") && !node.has("date") && !node.has("server")) {
            return null;
        }
        return ObjectMapperUtil.getObjectMapper().convertValue(node, SaleData.class);
    }

    private String text(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText(null);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record ParseResult(List<SaleData> records, List<PriceGroup> groups) {
    }

    public record PriceGroup(String name, List<SaleData> list) {
    }
}