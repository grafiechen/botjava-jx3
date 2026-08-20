package com.grafie.botjava.service;

import com.grafie.botjava.config.BotMongoProperties;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Lua 角色状态集合适配器。只查询既有文档或更新白名单字段，不创建文档、集合或索引。
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "bot.mongodb", name = "enabled", havingValue = "true")
public class LuaRoleStatusStore {

    private final MongoTemplate mongoTemplate;
    private final BotMongoProperties.RoleStatus properties;

    public LuaRoleStatusStore(MongoTemplate botMongoTemplate, BotMongoProperties properties) {
        this.mongoTemplate = botMongoTemplate;
        this.properties = properties.getRoleStatus();
    }

    public List<Document> findByServerAndRoleName(String server, String roleName) {
        Bson identityFilter = identityFilter(requireText(server, "区服"), requireText(roleName, "角色名"));
        List<Document> documents = collection().find(identityFilter)
                .limit(properties.getMaxMatches() + 1)
                .into(new ArrayList<>());
        ensureWithinLimit(documents.size());
        log.info("MongoDB 角色状态查询完成，matchCount=>{}", documents.size());
        return documents.stream().map(Document::new).toList();
    }

    public UpdateSummary updateByServerAndRoleName(String server, String roleName,
                                                   String fieldName, Object value) {
        List<Document> candidates = findByServerAndRoleName(server, roleName);
        if (candidates.isEmpty()) {
            return new UpdateSummary(0, 0);
        }
        List<Object> documentIds = candidates.stream()
                .map(document -> document.get("_id"))
                .filter(java.util.Objects::nonNull)
                .toList();
        if (documentIds.size() != candidates.size()) {
            throw new IllegalStateException("MongoDB 角色状态文档缺少 _id，已拒绝更新");
        }
        UpdateResult result = collection().updateMany(
                Filters.in("_id", documentIds), Updates.set(requireFieldName(fieldName), value));
        log.info("MongoDB 角色状态字段更新完成，candidateCount=>{}，matchedCount=>{}，modifiedCount=>{}",
                candidates.size(), result.getMatchedCount(), result.getModifiedCount());
        return new UpdateSummary(result.getMatchedCount(), result.getModifiedCount());
    }

    private MongoCollection<Document> collection() {
        return mongoTemplate.getCollection(properties.getCollection());
    }

    private Bson identityFilter(String server, String roleName) {
        return Filters.and(
                Filters.eq(properties.getServerField(), server),
                Filters.eq(properties.getRoleNameField(), roleName)
        );
    }

    private void ensureWithinLimit(int count) {
        if (count > properties.getMaxMatches()) {
            throw new TooManyMatchesException(properties.getMaxMatches());
        }
    }

    private String requireFieldName(String value) {
        String field = requireText(value, "MongoDB 字段名");
        if (field.contains(".") || field.startsWith("$") || field.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("MongoDB 字段名不合法");
        }
        return field;
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        return value.trim();
    }

    public record UpdateSummary(long matchedCount, long modifiedCount) {
    }

    public static class TooManyMatchesException extends RuntimeException {
        private final int maximum;

        public TooManyMatchesException(int maximum) {
            super("相同区服和角色名匹配记录超过上限 " + maximum);
            this.maximum = maximum;
        }

        public int getMaximum() {
            return maximum;
        }
    }
}