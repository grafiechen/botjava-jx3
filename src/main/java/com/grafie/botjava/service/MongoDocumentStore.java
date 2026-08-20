package com.grafie.botjava.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.config.BotMongoProperties;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * MongoDB 通用文档存储边界。具体业务应在自己的 Service 中固定集合名并封装字段语义。
 */
@Service
@ConditionalOnProperty(prefix = "bot.mongodb", name = "enabled", havingValue = "true")
public class MongoDocumentStore {

    private static final Pattern COLLECTION_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_.-]{0,119}$");
    private static final int MAX_DOCUMENT_ID_LENGTH = 200;

    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;
    private final BotMongoProperties properties;

    public MongoDocumentStore(MongoTemplate botMongoTemplate, ObjectMapper objectMapper,
                              BotMongoProperties properties) {
        this.mongoTemplate = botMongoTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public void upsert(String collectionName, String documentId, JsonNode payload) {
        String collection = requireCollectionName(collectionName);
        String id = requireDocumentId(documentId);
        Document payloadDocument = toPayloadDocument(payload);
        Date now = Date.from(Instant.now());
        Bson update = Updates.combine(
                Updates.set("payload", payloadDocument),
                Updates.set("updatedAt", now),
                Updates.setOnInsert("createdAt", now)
        );
        mongoTemplate.getCollection(collection).updateOne(
                Filters.eq("_id", id), update, new UpdateOptions().upsert(true));
    }

    public Optional<JsonNode> find(String collectionName, String documentId) {
        String collection = requireCollectionName(collectionName);
        String id = requireDocumentId(documentId);
        Document stored = mongoTemplate.getCollection(collection)
                .find(Filters.eq("_id", id))
                .projection(new Document("payload", 1).append("_id", 0))
                .first();
        if (stored == null || !(stored.get("payload") instanceof Document payload)) {
            return Optional.empty();
        }
        return Optional.of(objectMapper.valueToTree(payload));
    }

    public boolean exists(String collectionName, String documentId) {
        String collection = requireCollectionName(collectionName);
        String id = requireDocumentId(documentId);
        return mongoTemplate.getCollection(collection)
                .countDocuments(Filters.eq("_id", id), new com.mongodb.client.model.CountOptions().limit(1)) > 0;
    }

    public boolean delete(String collectionName, String documentId) {
        String collection = requireCollectionName(collectionName);
        String id = requireDocumentId(documentId);
        return mongoTemplate.getCollection(collection).deleteOne(Filters.eq("_id", id)).getDeletedCount() > 0;
    }

    private Document toPayloadDocument(JsonNode payload) {
        if (payload == null || !payload.isObject()) {
            throw new IllegalArgumentException("MongoDB payload 必须是 JSON object");
        }
        try {
            byte[] serialized = objectMapper.writeValueAsBytes(payload);
            if (serialized.length > properties.getMaxDocumentBytes()) {
                throw new IllegalArgumentException("MongoDB payload 超过允许大小："
                        + serialized.length + " > " + properties.getMaxDocumentBytes());
            }
            return Document.parse(new String(serialized, StandardCharsets.UTF_8));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("MongoDB payload 无法序列化", exception);
        }
    }

    private String requireCollectionName(String collectionName) {
        String cleaned = clean(collectionName);
        if (cleaned == null || !COLLECTION_PATTERN.matcher(cleaned).matches()
                || cleaned.startsWith("system.") || cleaned.contains("..")) {
            throw new IllegalArgumentException("MongoDB collection 名称不合法");
        }
        return cleaned;
    }

    private String requireDocumentId(String documentId) {
        String cleaned = clean(documentId);
        if (cleaned == null || cleaned.length() > MAX_DOCUMENT_ID_LENGTH
                || cleaned.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("MongoDB documentId 不合法");
        }
        return cleaned;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }
}