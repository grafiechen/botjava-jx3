package com.grafie.botjava.service;

import com.grafie.botjava.config.BotMongoProperties;
import com.grafie.botjava.entity.ScriptStatusFieldDefinition;
import com.grafie.botjava.mapper.ScriptStatusFieldDefinitionMapper;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ScriptStatusFieldService {

    private static final Set<String> PROTECTED_FIELDS = Set.of(
            "_id", "全局ID", "账号", "角色ID", "token", "password", "secret"
    );
    private static final String ROLE_INFO_QUERY_ALIAS = "角色信息";
    private static final List<ResolvedQueryField> ROLE_INFO_QUERY_FIELDS = List.of(
            new ResolvedQueryField("背包剩余空间", "背包剩余空间"),
            new ResolvedQueryField("角色金币", "金币"),
            new ResolvedQueryField("精力", "精力"),
            new ResolvedQueryField("侠行点", "侠义点"),
            new ResolvedQueryField("威望", "威望")
    );

    private final ScriptStatusFieldDefinitionMapper mapper;
    private final BotMongoProperties mongoProperties;

    public ScriptStatusFieldService(ScriptStatusFieldDefinitionMapper mapper,
                                    BotMongoProperties mongoProperties) {
        this.mapper = mapper;
        this.mongoProperties = mongoProperties;
    }

    public List<ScriptStatusFieldDefinition> enabledFields() {
        return List.copyOf(mapper.findByEnabledTrueOrderByGroupNameAscSortOrderAscIdAsc());
    }

    public List<ScriptStatusFieldDefinition> list() {
        return List.copyOf(mapper.findAllByOrderByGroupNameAscSortOrderAscIdAsc());
    }

    public ScriptStatusFieldDefinition findEnabled(String mongoFieldName) {
        ScriptStatusFieldDefinition definition = mapper.findByMongoFieldName(requireFieldName(mongoFieldName));
        return definition != null && definition.isEnabled() ? definition : null;
    }

    public List<ResolvedQueryField> resolveQueryFields(String requestedName) {
        String requested = requireQueryName(requestedName);
        String normalized = normalizeQueryName(requested);
        if (normalized.equals(normalizeQueryName(ROLE_INFO_QUERY_ALIAS))) {
            return ROLE_INFO_QUERY_FIELDS;
        }
        List<ScriptStatusFieldDefinition> enabled = enabledFields();

        List<ScriptStatusFieldDefinition> exactFields = enabled.stream()
                .filter(definition -> normalized.equals(normalizeQueryName(definition.getMongoFieldName())))
                .toList();
        if (!exactFields.isEmpty()) {
            return toResolvedQueryFields(exactFields);
        }

        List<ScriptStatusFieldDefinition> aliasFields = enabled.stream()
                .filter(definition -> normalized.equals(normalizeQueryName(definition.getDisplayName()))
                        || normalized.equals(normalizeQueryName(definition.getGroupName())))
                .toList();
        if (!aliasFields.isEmpty()) {
            return toResolvedQueryFields(aliasFields);
        }
        return List.of(new ResolvedQueryField(requested, requested));
    }

    @Transactional
    public ScriptStatusFieldDefinition save(String mongoFieldName, String displayName, String groupName,
                                            String type, String access, int sortOrder, String updatedBy) {
        String field = requireFieldName(mongoFieldName);
        ScriptStatusFieldDefinition definition = mapper.findByMongoFieldName(field);
        if (definition == null) {
            definition = new ScriptStatusFieldDefinition();
            definition.setMongoFieldName(field);
        }
        definition.setDisplayName(requireText(displayName, "显示名", 100));
        definition.setGroupName(requireText(groupName, "分组", 100));
        definition.setValueType(parseType(type));
        definition.setWritable(parseWritable(access));
        definition.setEnabled(true);
        definition.setSortOrder(sortOrder);
        definition.setUpdatedBy(limit(clean(updatedBy), 128));
        return mapper.save(definition);
    }

    @Transactional
    public boolean delete(String mongoFieldName) {
        ScriptStatusFieldDefinition definition = mapper.findByMongoFieldName(requireFieldName(mongoFieldName));
        if (definition == null) {
            return false;
        }
        mapper.delete(definition);
        return true;
    }

    public Object parseValue(ScriptStatusFieldDefinition definition, String rawValue) {
        String value = requireText(rawValue, "字段值", 500);
        try {
            return switch (definition.getValueType()) {
                case TEXT -> value;
                case INTEGER -> Long.valueOf(value);
                case DECIMAL -> new BigDecimal(value);
                case BOOLEAN -> parseBoolean(value);
                case DATETIME -> Date.from(LocalDateTime.parse(value).atZone(ZoneId.systemDefault()).toInstant());
            };
        } catch (NumberFormatException | DateTimeParseException exception) {
            throw new IllegalArgumentException("字段“" + definition.getDisplayName()
                    + "”需要 " + typeHint(definition.getValueType()) + "。", exception);
        }
    }

    private Boolean parseBoolean(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "true", "1", "是", "开启", "完成" -> true;
            case "false", "0", "否", "关闭", "未完成" -> false;
            default -> throw new IllegalArgumentException("布尔值只支持 true/false、1/0、是/否、开启/关闭或完成/未完成。");
        };
    }

    private ScriptStatusFieldDefinition.ValueType parseType(String value) {
        try {
            return ScriptStatusFieldDefinition.ValueType.valueOf(requireText(value, "类型", 20).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("类型只支持 TEXT、INTEGER、DECIMAL、BOOLEAN、DATETIME。");
        }
    }

    private boolean parseWritable(String access) {
        return switch (requireText(access, "读写属性", 20).toUpperCase(Locale.ROOT)) {
            case "WRITE", "WRITABLE", "可写" -> true;
            case "READ", "READONLY", "只读" -> false;
            default -> throw new IllegalArgumentException("读写属性只支持 READ/WRITE 或 只读/可写。");
        };
    }

    private List<ResolvedQueryField> toResolvedQueryFields(List<ScriptStatusFieldDefinition> definitions) {
        Map<String, ResolvedQueryField> fields = new LinkedHashMap<>();
        for (ScriptStatusFieldDefinition definition : definitions) {
            String mongoFieldName = definition.getMongoFieldName();
            if (mongoFieldName == null) {
                continue;
            }
            String displayName = clean(definition.getDisplayName());
            fields.putIfAbsent(mongoFieldName, new ResolvedQueryField(
                    mongoFieldName, displayName == null ? mongoFieldName : displayName));
        }
        return List.copyOf(fields.values());
    }

    private String requireQueryName(String value) {
        return requireText(value, "查询字段或别名", 100);
    }

    private String normalizeQueryName(String value) {
        String cleaned = clean(value);
        if (cleaned == null) {
            return "";
        }
        return Normalizer.normalize(cleaned, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\s._\\-·]+", "");
    }

    private String requireFieldName(String value) {
        String field = requireText(value, "MongoDB 字段名", 100);
        if (field.contains(".") || field.startsWith("$") || field.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("MongoDB 字段名只允许顶层普通字段，不能包含点号或以 $ 开头。");
        }
        if (PROTECTED_FIELDS.stream().anyMatch(item -> item.equalsIgnoreCase(field))
                || field.equalsIgnoreCase(mongoProperties.getRoleStatus().getServerField())
                || field.equalsIgnoreCase(mongoProperties.getRoleStatus().getRoleNameField())) {
            throw new IllegalArgumentException("该字段属于内部或敏感字段，不能配置。");
        }
        return field;
    }

    private String requireText(String value, String name, int maxLength) {
        String cleaned = clean(value);
        if (cleaned == null) {
            throw new IllegalArgumentException(name + "不能为空。");
        }
        if (cleaned.length() > maxLength) {
            throw new IllegalArgumentException(name + "不能超过 " + maxLength + " 个字符。");
        }
        return cleaned;
    }

    private String typeHint(ScriptStatusFieldDefinition.ValueType type) {
        return switch (type) {
            case TEXT -> "文本";
            case INTEGER -> "整数";
            case DECIMAL -> "数字";
            case BOOLEAN -> "布尔值";
            case DATETIME -> "ISO-8601 日期时间，例如 2026-08-12T21:30:00";
        };
    }

    private String clean(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
    private String limit(String value, int max) { return value == null || value.length() <= max ? value : value.substring(0, max); }

    public record ResolvedQueryField(String mongoFieldName, String displayName) {
    }
}
