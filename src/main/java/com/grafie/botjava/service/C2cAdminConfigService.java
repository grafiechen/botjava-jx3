package com.grafie.botjava.service;

import com.grafie.botjava.config.QqAdminProperties;
import com.grafie.botjava.entity.BotAdminAuditLog;
import com.grafie.botjava.entity.BotRuntimeConfig;
import com.grafie.botjava.entity.ScriptStatusFieldDefinition;
import com.grafie.botjava.entity.dto.c2c.C2cMessageCreateDto;
import com.grafie.botjava.util.SensitiveDataUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class C2cAdminConfigService {

    private static final String SOURCE = "C2C";
    private static final String CATEGORY_CONFIG = "CONFIG";
    private static final String CATEGORY_REVIEW = "REVIEW";
    private static final String CATEGORY_SCRIPT_FIELD = "SCRIPT_FIELD";

    private final QqAdminProperties adminProperties;
    private final BotRuntimeConfigService configService;
    private final BotAdminAuditService auditService;
    private final C2cMessageSender c2cMessageSender;
    private final ScriptStatusFieldService scriptStatusFieldService;

    public C2cAdminConfigService(QqAdminProperties adminProperties,
                                 BotRuntimeConfigService configService,
                                 BotAdminAuditService auditService,
                                 C2cMessageSender c2cMessageSender,
                                 ScriptStatusFieldService scriptStatusFieldService) {
        this.adminProperties = adminProperties;
        this.configService = configService;
        this.auditService = auditService;
        this.c2cMessageSender = c2cMessageSender;
        this.scriptStatusFieldService = scriptStatusFieldService;
    }

    public void handle(C2cMessageCreateDto message) {
        String userOpenId = message == null ? null : message.userOpenId();
        String content = message == null || message.getContent() == null ? "" : message.getContent().trim();
        if (userOpenId == null || userOpenId.isBlank()) {
            log.warn("C2C 管理消息缺少 userOpenId，忽略处理");
            return;
        }
        if (!isAdminCommand(content)) {
            reply(message, "当前私聊只支持主号配置管理。发送：配置帮助");
            return;
        }
        if (!adminProperties.isMasterOpenid(userOpenId)) {
            auditService.record(category(content), commandName(content), targetKey(content), userOpenId,
                    SOURCE, false, "非主号尝试执行配置管理");
            reply(message, "没有权限。只有配置的 QQ 主号可以执行私聊配置管理。");
            return;
        }
        try {
            String response = execute(userOpenId, content);
            reply(message, response);
        } catch (RuntimeException e) {
            auditService.record(category(content), commandName(content), targetKey(content), userOpenId,
                    SOURCE, false, SensitiveDataUtil.summarize(e));
            reply(message, "执行失败：" + e.getMessage());
        }
    }

    private String execute(String userOpenId, String content) {
        if (content.equals("配置帮助") || content.equals("管理帮助")) {
            return help();
        }
        if (content.equals("脚本字段列表")) {
            auditService.record(CATEGORY_SCRIPT_FIELD, "LIST", null, userOpenId, SOURCE, true, null);
            return listScriptFields();
        }
        if (content.startsWith("脚本字段设置 ") || content.startsWith("脚本字段添加 ")) {
            String prefix = content.startsWith("脚本字段设置 ") ? "脚本字段设置 " : "脚本字段添加 ";
            String[] parts = content.substring(prefix.length()).trim().split("\\s+");
            if (parts.length != 6) {
                throw new IllegalArgumentException("格式：脚本字段设置 Mongo字段 显示名 分组 类型 READ|WRITE 排序");
            }
            int sortOrder;
            try {
                sortOrder = Integer.parseInt(parts[5]);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("排序必须是整数。");
            }
            if (sortOrder < -100000 || sortOrder > 100000) {
                throw new IllegalArgumentException("排序必须在 -100000 到 100000 之间。");
            }
            ScriptStatusFieldDefinition definition = scriptStatusFieldService.save(
                    parts[0], parts[1], parts[2], parts[3], parts[4], sortOrder, userOpenId);
            auditService.record(CATEGORY_SCRIPT_FIELD, "SET", definition.getMongoFieldName(),
                    userOpenId, SOURCE, true, null);
            return "已设置脚本字段：" + definition.getMongoFieldName()
                    + " -> " + definition.getDisplayName()
                    + "（" + (definition.isWritable() ? "可写" : "只读") + "）";
        }
        if (content.startsWith("脚本字段删除 ")) {
            String fieldName = content.substring("脚本字段删除 ".length()).trim();
            boolean deleted = scriptStatusFieldService.delete(fieldName);
            auditService.record(CATEGORY_SCRIPT_FIELD, "DELETE", fieldName, userOpenId, SOURCE, true, null);
            return deleted ? "已删除脚本字段：" + fieldName : "脚本字段不存在：" + fieldName;
        }
        if (content.equals("配置列表")) {
            auditService.record(CATEGORY_CONFIG, "LIST", null, userOpenId, SOURCE, true, null);
            return listConfigs();
        }
        if (content.startsWith("配置查看 ")) {
            String key = content.substring("配置查看 ".length()).trim();
            auditService.record(CATEGORY_CONFIG, "GET", key, userOpenId, SOURCE, true, null);
            return configService.find(key).map(this::displayConfigValue).orElse("配置不存在：" + key);
        }
        if (content.startsWith("配置设置 ")) {
            String rest = content.substring("配置设置 ".length()).trim();
            int split = rest.indexOf(' ');
            if (split <= 0) {
                throw new IllegalArgumentException("格式：配置设置 key value");
            }
            String key = rest.substring(0, split).trim();
            String value = rest.substring(split + 1).trim();
            BotRuntimeConfig config = configService.set(key, value, userOpenId);
            auditService.record(CATEGORY_CONFIG, "SET", key, userOpenId, SOURCE, true, null);
            return "已设置配置：" + config.getConfigKey();
        }
        if (content.startsWith("配置删除 ")) {
            String key = content.substring("配置删除 ".length()).trim();
            boolean deleted = configService.delete(key);
            auditService.record(CATEGORY_CONFIG, "DELETE", key, userOpenId, SOURCE, true, null);
            return deleted ? "已删除配置：" + key : "配置不存在：" + key;
        }
        if (content.equals("配置日志") || content.equals("审核日志")) {
            auditService.record(CATEGORY_REVIEW, "LIST_AUDIT", null, userOpenId, SOURCE, true, null);
            return auditLogs();
        }
        throw new IllegalArgumentException("未知管理指令。发送：配置帮助");
    }

    private String help() {
        return "配置帮助\n"
                + "配置列表\n"
                + "配置查看 key\n"
                + "配置设置 key value\n"
                + "配置删除 key\n"
                + "脚本字段列表\n"
                + "脚本字段设置 Mongo字段 显示名 分组 TEXT|INTEGER|DECIMAL|BOOLEAN|DATETIME READ|WRITE 排序\n"
                + "脚本字段删除 Mongo字段\n"
                + "配置日志";
    }

    private String listScriptFields() {
        List<ScriptStatusFieldDefinition> fields = scriptStatusFieldService.list();
        if (fields.isEmpty()) {
            return "暂无脚本状态字段配置";
        }
        StringBuilder builder = new StringBuilder("脚本状态字段：");
        for (ScriptStatusFieldDefinition field : fields) {
            builder.append('\n').append(field.getMongoFieldName())
                    .append(" -> ").append(field.getDisplayName())
                    .append(" / ").append(field.getGroupName())
                    .append(" / ").append(field.getValueType())
                    .append(" / ").append(field.isWritable() ? "可写" : "只读")
                    .append(" / ").append(field.getSortOrder());
        }
        return builder.toString();
    }

    private String listConfigs() {
        List<BotRuntimeConfig> configs = configService.list();
        if (configs.isEmpty()) { return "暂无运行时配置"; }
        StringBuilder builder = new StringBuilder("运行时配置：");
        for (BotRuntimeConfig config : configs) { builder.append('\n').append(config.getConfigKey()); }
        return builder.toString();
    }

    private String auditLogs() {
        List<BotAdminAuditLog> logs = auditService.recent();
        if (logs.isEmpty()) { return "暂无配置/审核日志"; }
        StringBuilder builder = new StringBuilder("最近配置/审核日志：");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm");
        for (BotAdminAuditLog log : logs) {
            builder.append('\n').append(log.getCreateTime() == null ? "--" : formatter.format(log.getCreateTime()))
                    .append(' ').append(log.getCategory()).append('/').append(log.getAction())
                    .append(' ').append(log.getTargetKey() == null ? "-" : log.getTargetKey())
                    .append(' ').append(Boolean.TRUE.equals(log.getSuccess()) ? "成功" : "失败");
        }
        return builder.toString();
    }

    private void reply(C2cMessageCreateDto message, String content) { c2cMessageSender.replyText(message, content); }

    private String displayConfigValue(BotRuntimeConfig config) {
        return isSensitiveConfigKey(config.getConfigKey())
                ? config.getConfigKey() + "=******"
                : config.getConfigKey() + "=" + config.getConfigValue();
    }

    private boolean isSensitiveConfigKey(String key) {
        if (key == null) { return false; }
        String normalized = key.toLowerCase();
        return normalized.contains("token") || normalized.contains("secret")
                || normalized.contains("password") || normalized.contains("credential")
                || normalized.contains("authorization") || normalized.contains("accesskey")
                || normalized.contains("privatekey") || normalized.contains("appkey")
                || normalized.contains("ticket");
    }

    private boolean isAdminCommand(String content) {
        return content.equals("配置帮助") || content.equals("管理帮助") || content.equals("配置列表")
                || content.equals("配置日志") || content.equals("审核日志")
                || content.equals("脚本字段列表") || content.startsWith("脚本字段设置 ")
                || content.startsWith("脚本字段添加 ") || content.startsWith("脚本字段删除 ")
                || content.startsWith("配置查看 ") || content.startsWith("配置设置 ")
                || content.startsWith("配置删除 ");
    }

    private String category(String content) {
        return content != null && content.startsWith("脚本字段") ? CATEGORY_SCRIPT_FIELD : CATEGORY_CONFIG;
    }

    private String commandName(String content) {
        if (content == null || content.isBlank()) { return "UNKNOWN"; }
        int split = content.trim().indexOf(' ');
        return split < 0 ? content.trim() : content.trim().substring(0, split);
    }

    private String targetKey(String content) {
        if (content == null) { return null; }
        String trimmed = content.trim();
        for (String prefix : List.of("配置查看 ", "配置设置 ", "配置删除 ",
                "脚本字段设置 ", "脚本字段添加 ", "脚本字段删除 ")) {
            if (trimmed.startsWith(prefix)) {
                String rest = trimmed.substring(prefix.length()).trim();
                int split = rest.indexOf(' ');
                return split < 0 ? rest : rest.substring(0, split);
            }
        }
        return null;
    }
}