package com.grafie.botjava.service;

import com.grafie.botjava.entity.GroupDailyPushField;
import com.grafie.botjava.entity.ScriptStatusFieldDefinition;
import com.grafie.botjava.mapper.GroupDailyPushFieldMapper;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class GroupDailyPushFieldService {
    private static final int MAX_FIELDS = 12;
    private static final Set<String> PROTECTED_FIELDS = Set.of(
            "_id", "全局ID", "账号", "角色ID", "token", "password", "secret", "服务器", "角色名");
    private final GroupDailyPushFieldMapper mapper;

    public GroupDailyPushFieldService(GroupDailyPushFieldMapper mapper) {
        this.mapper = mapper;
    }

    public List<GroupDailyPushField> enabledFields(String groupOpenId) {
        String group = requireText(groupOpenId, "群标识", 128);
        java.util.Map<String, GroupDailyPushField> merged = new java.util.LinkedHashMap<>();
        for (GroupDailyPushField field : defaultFields(group)) {
            merged.put(field.getMongoFieldName(), field);
        }
        List<GroupDailyPushField> overrides = mapper.findByGroupOpenIdOrderBySortOrderAscIdAsc(group);
        if (overrides != null) {
            for (GroupDailyPushField override : overrides) {
                if (override.isEnabled()) {
                    merged.put(override.getMongoFieldName(), override);
                } else {
                    merged.remove(override.getMongoFieldName());
                }
            }
        }
        return merged.values().stream()
                .sorted(java.util.Comparator.comparingInt(GroupDailyPushField::getSortOrder)
                        .thenComparing(field -> field.getId() == null ? Long.MAX_VALUE : field.getId()))
                .toList();
    }
    public boolean hasCustomConfiguration(String groupOpenId) {
        return mapper.existsByGroupOpenId(requireText(groupOpenId, "群标识", 128));
    }

    @Transactional
    public GroupDailyPushField save(String groupOpenId, String mongoFieldName, String displayName,
                                    String valueType, int sortOrder, String updatedBy) {
        String group = requireText(groupOpenId, "群标识", 128);
        String field = requireFieldName(mongoFieldName);
        GroupDailyPushField definition = mapper.findByGroupOpenIdAndMongoFieldName(group, field);
        if (definition == null) {
            if (enabledFields(group).size() >= MAX_FIELDS) {
                throw new IllegalArgumentException("每个群最多配置 " + MAX_FIELDS + " 个日常推送字段。");
            }
            definition = new GroupDailyPushField();
            definition.setGroupOpenId(group);
            definition.setMongoFieldName(field);
        }
        definition.setDisplayName(requireText(displayName, "显示名", 100));
        definition.setValueType(parseType(valueType));
        definition.setSortOrder(Math.max(0, Math.min(sortOrder, 10000)));
        definition.setEnabled(true);
        definition.setUpdatedBy(limit(clean(updatedBy), 128));
        return mapper.save(definition);
    }

    @Transactional
    public boolean disable(String groupOpenId, String mongoFieldName, String updatedBy) {
        String group = requireText(groupOpenId, "群标识", 128);
        GroupDailyPushField definition = mapper.findByGroupOpenIdAndMongoFieldName(
                group, requireFieldName(mongoFieldName));
        if (definition == null) {
            GroupDailyPushField defaultField = defaultFields(group).stream()
                    .filter(field -> field.getMongoFieldName().equals(requireFieldName(mongoFieldName)))
                    .findFirst().orElse(null);
            if (defaultField == null) {
                return false;
            }
            definition = defaultField;
        }
        definition.setEnabled(false);
        definition.setUpdatedBy(limit(clean(updatedBy), 128));
        mapper.save(definition);
        return true;
    }

    private List<GroupDailyPushField> defaultFields(String groupOpenId) {
        return List.of(
                defaultField(groupOpenId, "每日签到任务", "上次日常完成", ScriptStatusFieldDefinition.ValueType.DATETIME, 10),
                defaultField(groupOpenId, "角色金币", "金币", ScriptStatusFieldDefinition.ValueType.INTEGER, 20),
                defaultField(groupOpenId, "精力", "精力", ScriptStatusFieldDefinition.ValueType.INTEGER, 30),
                defaultField(groupOpenId, "侠行点", "侠义", ScriptStatusFieldDefinition.ValueType.INTEGER, 40)
        );
    }

    private GroupDailyPushField defaultField(String groupOpenId, String field, String display,
                                             ScriptStatusFieldDefinition.ValueType type, int order) {
        GroupDailyPushField value = new GroupDailyPushField();
        value.setGroupOpenId(groupOpenId);
        value.setMongoFieldName(field);
        value.setDisplayName(display);
        value.setValueType(type);
        value.setSortOrder(order);
        value.setEnabled(true);
        return value;
    }

    private ScriptStatusFieldDefinition.ValueType parseType(String value) {
        try {
            return ScriptStatusFieldDefinition.ValueType.valueOf(
                    requireText(value == null ? "TEXT" : value, "类型", 20).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("类型只支持 TEXT、INTEGER、DECIMAL、BOOLEAN、DATETIME。");
        }
    }

    private String requireFieldName(String value) {
        String field = requireText(value, "MongoDB 字段名", 100);
        if (field.contains(".") || field.startsWith("$") || field.indexOf('\0') >= 0
                || PROTECTED_FIELDS.stream().anyMatch(item -> item.equalsIgnoreCase(field))) {
            throw new IllegalArgumentException("该 MongoDB 字段名不允许用于推送配置。");
        }
        return field;
    }

    private String requireText(String value, String name, int maximum) {
        String cleaned = clean(value);
        if (cleaned == null) {
            throw new IllegalArgumentException(name + "不能为空。");
        }
        if (cleaned.length() > maximum) {
            throw new IllegalArgumentException(name + "不能超过 " + maximum + " 个字符。");
        }
        return cleaned;
    }

    private String clean(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
    private String limit(String value, int maximum) { return value == null || value.length() <= maximum ? value : value.substring(0, maximum); }
}