package com.grafie.botjava.service;

import com.grafie.botjava.config.BotMongoProperties;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    public List<RoleBagSpaceRecord> findAllRoleBagSpaces() {
        List<Document> documents = collection().find()
                .projection(Projections.fields(Projections.include(
                        properties.getServerField(),
                        properties.getRoleNameField(),
                        properties.getBagRemainingField()), Projections.excludeId()))
                .into(new ArrayList<>());
        log.info("MongoDB 背包空间全量查询完成，documentCount=>{}", documents.size());
        return documents.stream()
                .map(document -> new RoleBagSpaceRecord(
                        document.get(properties.getServerField()),
                        document.get(properties.getRoleNameField()),
                        document.get(properties.getBagRemainingField())))
                .toList();
    }

    public List<RoleFieldRecord> findAllRoleFields(List<String> fieldNames) {
        List<String> fields = fieldNames == null ? List.of() : fieldNames.stream()
                .map(this::requireFieldName)
                .distinct()
                .toList();
        if (fields.isEmpty()) {
            throw new IllegalArgumentException("查询字段不能为空");
        }
        List<String> projectionFields = new ArrayList<>();
        projectionFields.add(properties.getServerField());
        projectionFields.add(properties.getRoleNameField());
        projectionFields.addAll(fields);
        projectionFields = projectionFields.stream().distinct().toList();
        List<Bson> existenceFilters = fields.stream()
                .map(field -> Filters.exists(field, true))
                .toList();
        Bson filter = existenceFilters.size() == 1
                ? existenceFilters.getFirst() : Filters.or(existenceFilters);
        List<Document> documents = collection().find(filter)
                .projection(Projections.fields(
                        Projections.include(projectionFields), Projections.excludeId()))
                .into(new ArrayList<>());
        log.info("MongoDB 指定字段全量查询完成，fieldCount=>{}，documentCount=>{}",
                fields.size(), documents.size());
        return documents.stream().map(document -> {
            Map<String, Object> values = new LinkedHashMap<>();
            fields.forEach(field -> values.put(field, document.get(field)));
            return new RoleFieldRecord(
                    document.get(properties.getServerField()),
                    document.get(properties.getRoleNameField()),
                    values);
        }).toList();
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

    public record RoleBagSpaceRecord(Object server, Object roleName, Object remainingSpace) {
    }

    public record RoleFieldRecord(Object server, Object roleName, Map<String, Object> values) {
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