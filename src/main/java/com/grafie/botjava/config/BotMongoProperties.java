package com.grafie.botjava.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

import java.util.regex.Pattern;

@ConfigurationProperties(prefix = "bot.mongodb")
public class BotMongoProperties {

    private static final Pattern COLLECTION_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_.-]{0,119}$");
    private static final Pattern FIELD_PATTERN = Pattern.compile("^[^.$\\x00]{1,100}$");

    private boolean enabled;
    private String uri;
    private String database = "botjava";
    private int maxDocumentBytes = 1024 * 1024;
    private RoleStatus roleStatus = new RoleStatus();

    @PostConstruct
    public void validate() {
        Assert.hasText(database, "bot.mongodb.database 不能为空");
        Assert.isTrue(database.matches("^[A-Za-z0-9_-]{1,64}$"),
                "bot.mongodb.database 只能包含字母、数字、下划线和短横线");
        Assert.isTrue(maxDocumentBytes >= 1024 && maxDocumentBytes <= 8 * 1024 * 1024,
                "bot.mongodb.max-document-bytes 必须在 1KB 到 8MB 之间");
        Assert.notNull(roleStatus, "bot.mongodb.role-status 不能为空");
        Assert.isTrue(COLLECTION_PATTERN.matcher(roleStatus.collection).matches()
                        && !roleStatus.collection.startsWith("system.")
                        && !roleStatus.collection.contains(".."),
                "bot.mongodb.role-status.collection 不合法");
        Assert.isTrue(FIELD_PATTERN.matcher(roleStatus.serverField).matches(),
                "bot.mongodb.role-status.server-field 不合法");
        Assert.isTrue(FIELD_PATTERN.matcher(roleStatus.roleNameField).matches(),
                "bot.mongodb.role-status.role-name-field 不合法");
        Assert.isTrue(roleStatus.maxMatches >= 1 && roleStatus.maxMatches <= 100,
                "bot.mongodb.role-status.max-matches 必须在 1 到 100 之间");
        if (enabled) {
            Assert.hasText(uri, "启用 MongoDB 时 bot.mongodb.uri 不能为空");
            Assert.isTrue(uri.startsWith("mongodb://") || uri.startsWith("mongodb+srv://"),
                    "bot.mongodb.uri 必须使用 mongodb:// 或 mongodb+srv://");
        }
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getUri() { return uri; }
    public void setUri(String uri) { this.uri = clean(uri); }
    public String getDatabase() { return database; }
    public void setDatabase(String database) { this.database = clean(database); }
    public int getMaxDocumentBytes() { return maxDocumentBytes; }
    public void setMaxDocumentBytes(int maxDocumentBytes) { this.maxDocumentBytes = maxDocumentBytes; }
    public RoleStatus getRoleStatus() { return roleStatus; }
    public void setRoleStatus(RoleStatus roleStatus) { this.roleStatus = roleStatus; }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    public static class RoleStatus {
        private String collection = "roles";
        private String serverField = "服务器";
        private String roleNameField = "角色名";
        private int maxMatches = 20;

        public String getCollection() { return collection; }
        public void setCollection(String collection) { this.collection = clean(collection); }
        public String getServerField() { return serverField; }
        public void setServerField(String serverField) { this.serverField = clean(serverField); }
        public String getRoleNameField() { return roleNameField; }
        public void setRoleNameField(String roleNameField) { this.roleNameField = clean(roleNameField); }
        public int getMaxMatches() { return maxMatches; }
        public void setMaxMatches(int maxMatches) { this.maxMatches = maxMatches; }

        private static String clean(String value) {
            return value == null ? null : value.trim();
        }
    }
}