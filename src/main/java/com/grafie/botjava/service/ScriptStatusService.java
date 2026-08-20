package com.grafie.botjava.service;

import com.grafie.botjava.entity.ScriptStatusFieldDefinition;
import com.grafie.botjava.entity.UserRoleBinding;
import com.grafie.botjava.entity.dto.common.AuthorDto;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.util.SensitiveDataUtil;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.lang.reflect.Array;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ScriptStatusService {

    private static final int MAX_DISPLAY_VALUE_LENGTH = 300;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ObjectProvider<LuaRoleStatusStore> storeProvider;
    private final ScriptStatusFieldService fieldService;
    private final UserCommandPreferenceService preferenceService;
    private final BotAdminAuditService auditService;

    public ScriptStatusService(ObjectProvider<LuaRoleStatusStore> storeProvider,
                               ScriptStatusFieldService fieldService,
                               UserCommandPreferenceService preferenceService,
                               BotAdminAuditService auditService) {
        this.storeProvider = storeProvider;
        this.fieldService = fieldService;
        this.preferenceService = preferenceService;
        this.auditService = auditService;
    }

    public BotResponse query(GroupAtMessageCreateDto message, String server, String roleName) {
        RoleIdentity identity = requireOwnedRole(message, server, roleName);
        if (identity == null) {
            return BotResponse.text("只能查询自己已绑定的角色，请先使用：绑定角色 区服 角色名 门派");
        }
        LuaRoleStatusStore store = storeProvider.getIfAvailable();
        if (store == null) {
            return BotResponse.text("脚本状态功能未开启，请联系管理员配置 MongoDB。");
        }
        List<ScriptStatusFieldDefinition> fields = fieldService.enabledFields();
        if (fields.isEmpty()) {
            return BotResponse.text("管理员尚未配置脚本状态展示字段。");
        }
        try {
            List<Document> documents = store.findByServerAndRoleName(identity.server(), identity.roleName());
            if (documents.isEmpty()) {
                return BotResponse.text("未找到该角色的脚本状态。");
            }
            return BotResponse.image("脚本状态", buildTemplateData(identity, documents, fields));
        } catch (LuaRoleStatusStore.TooManyMatchesException exception) {
            log.warn("MongoDB 角色状态匹配数量超过安全上限，maximum=>{}", exception.getMaximum());
            return BotResponse.text("该角色匹配的脚本记录过多，请联系管理员整理数据后重试。");
        } catch (RuntimeException exception) {
            log.error("查询 MongoDB 角色状态失败，reason=>{}", SensitiveDataUtil.summarize(exception), exception);
            return BotResponse.text("脚本状态查询失败，请稍后重试。");
        }
    }

    public BotResponse update(GroupAtMessageCreateDto message, String server, String roleName,
                              String fieldName, String rawValue) {
        String actorOpenId = memberOpenId(message);
        RoleIdentity identity = requireOwnedRole(message, server, roleName);
        if (identity == null) {
            recordUpdate(fieldName, actorOpenId, false, "角色未绑定到当前用户");
            return BotResponse.text("只能修改自己已绑定的角色，请先使用：绑定角色 区服 角色名 门派");
        }
        ScriptStatusFieldDefinition definition;
        try {
            definition = fieldService.findEnabled(fieldName);
        } catch (IllegalArgumentException exception) {
            recordUpdate(fieldName, actorOpenId, false, exception.getMessage());
            return BotResponse.text(exception.getMessage());
        }
        if (definition == null) {
            recordUpdate(fieldName, actorOpenId, false, "字段未配置");
            return BotResponse.text("该脚本字段未配置，不能修改。");
        }
        if (!definition.isWritable()) {
            recordUpdate(fieldName, actorOpenId, false, "字段只读");
            return BotResponse.text("字段“" + definition.getDisplayName() + "”是只读字段。");
        }
        LuaRoleStatusStore store = storeProvider.getIfAvailable();
        if (store == null) {
            recordUpdate(fieldName, actorOpenId, false, "MongoDB 未启用");
            return BotResponse.text("脚本状态功能未开启，请联系管理员配置 MongoDB。");
        }
        try {
            Object value = fieldService.parseValue(definition, rawValue);
            LuaRoleStatusStore.UpdateSummary result = store.updateByServerAndRoleName(
                    identity.server(), identity.roleName(), definition.getMongoFieldName(), value);
            if (result.matchedCount() == 0) {
                recordUpdate(fieldName, actorOpenId, false, "MongoDB 未找到角色记录");
                return BotResponse.text("未找到该角色的脚本记录，未执行修改。");
            }
            recordUpdate(fieldName, actorOpenId, true, null);
            return BotResponse.text("已更新“" + definition.getDisplayName() + "”，影响 "
                    + result.matchedCount() + " 条脚本记录。");
        } catch (LuaRoleStatusStore.TooManyMatchesException exception) {
            recordUpdate(fieldName, actorOpenId, false, "匹配数量超过安全上限");
            return BotResponse.text("该角色匹配的脚本记录过多，已拒绝修改，请联系管理员整理数据。");
        } catch (IllegalArgumentException exception) {
            recordUpdate(fieldName, actorOpenId, false, exception.getMessage());
            return BotResponse.text(exception.getMessage());
        } catch (RuntimeException exception) {
            recordUpdate(fieldName, actorOpenId, false, SensitiveDataUtil.summarize(exception));
            log.error("更新 MongoDB 角色状态失败，field=>{}，reason=>{}",
                    definition.getMongoFieldName(), SensitiveDataUtil.summarize(exception), exception);
            return BotResponse.text("脚本设置更新失败，请稍后重试。");
        }
    }

    private Map<String, Object> buildTemplateData(RoleIdentity identity, List<Document> documents,
                                                   List<ScriptStatusFieldDefinition> definitions) {
        List<Map<String, Object>> records = new ArrayList<>();
        for (int index = 0; index < documents.size(); index++) {
            Document document = documents.get(index);
            Map<String, List<Map<String, String>>> groups = new LinkedHashMap<>();
            for (ScriptStatusFieldDefinition definition : definitions) {
                groups.computeIfAbsent(definition.getGroupName(), ignored -> new ArrayList<>())
                        .add(Map.of(
                                "name", definition.getDisplayName(),
                                "value", displayValue(document.get(definition.getMongoFieldName()))
                        ));
            }
            List<Map<String, Object>> sections = new ArrayList<>();
            groups.forEach((name, values) -> sections.add(Map.of("name", name, "values", values)));
            records.add(Map.of("index", index + 1, "sections", sections));
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("server", identity.server());
        data.put("roleName", identity.roleName());
        data.put("matchCount", documents.size());
        data.put("records", records);
        return data;
    }

    private RoleIdentity requireOwnedRole(GroupAtMessageCreateDto message, String server, String roleName) {
        String cleanedServer = clean(server);
        String cleanedRoleName = clean(roleName);
        if (cleanedServer == null || cleanedRoleName == null) {
            return null;
        }
        UserCommandPreferenceService.BindingSnapshot snapshot;
        try {
            snapshot = preferenceService.findBindings(message);
        } catch (IllegalArgumentException exception) {
            return null;
        }
        boolean owned = snapshot.roles().stream().anyMatch(role -> sameRole(role, cleanedServer, cleanedRoleName));
        return owned ? new RoleIdentity(cleanedServer, cleanedRoleName) : null;
    }

    private boolean sameRole(UserRoleBinding role, String server, String roleName) {
        return role != null && server.equals(role.getServer()) && roleName.equals(role.getRoleName());
    }

    private String displayValue(Object value) {
        if (value == null) {
            return "未上报";
        }
        String text;
        if (value instanceof Date date) {
            text = DATE_TIME_FORMATTER.format(date.toInstant().atZone(ZoneId.systemDefault()));
        } else if (value instanceof Instant instant) {
            text = DATE_TIME_FORMATTER.format(instant.atZone(ZoneId.systemDefault()));
        } else if (value instanceof LocalDateTime localDateTime) {
            text = DATE_TIME_FORMATTER.format(localDateTime);
        } else if (value instanceof Boolean bool) {
            text = bool ? "是" : "否";
        } else if (value instanceof Collection<?> collection) {
            text = collection.stream().map(this::displayValue).reduce((left, right) -> left + "、" + right).orElse("未上报");
        } else if (value.getClass().isArray()) {
            List<String> parts = new ArrayList<>();
            for (int i = 0; i < Array.getLength(value); i++) {
                parts.add(displayValue(Array.get(value, i)));
            }
            text = String.join("、", parts);
        } else {
            text = String.valueOf(value);
        }
        String cleaned = clean(text);
        if (cleaned == null) {
            return "未上报";
        }
        return cleaned.length() <= MAX_DISPLAY_VALUE_LENGTH
                ? cleaned : cleaned.substring(0, MAX_DISPLAY_VALUE_LENGTH - 3) + "...";
    }

    private String memberOpenId(GroupAtMessageCreateDto message) {
        AuthorDto author = message == null ? null : message.getAuthor();
        return author == null ? null : clean(author.getMemberOpenid() != null
                ? author.getMemberOpenid() : author.getId());
    }

    private void recordUpdate(String fieldName, String actorOpenId, boolean success, String failure) {
        try {
            auditService.record("SCRIPT_STATUS", "UPDATE", clean(fieldName), actorOpenId,
                    "GROUP", success, failure);
        } catch (RuntimeException exception) {
            log.error("记录脚本状态更新审计失败，reason=>{}", SensitiveDataUtil.summarize(exception), exception);
        }
    }

    private String clean(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private record RoleIdentity(String server, String roleName) {
    }
}