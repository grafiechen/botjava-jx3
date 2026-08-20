package com.grafie.botjava.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.config.BotMongoProperties;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MongoDocumentStoreTest {

    private final MongoTemplate mongoTemplate = mock(MongoTemplate.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BotMongoProperties properties = new BotMongoProperties();
    private final MongoDocumentStore store = new MongoDocumentStore(mongoTemplate, objectMapper, properties);

    @Test
    void shouldUpsertStructuredJsonDocument() {
        MongoCollection<Document> collection = collection("jx3_price_snapshot");
        JsonNode payload = objectMapper.createObjectNode()
                .put("name", "金发·因陀罗")
                .put("server", "乾坤一掷");

        store.upsert("jx3_price_snapshot", "item:1001", payload);

        ArgumentCaptor<Bson> filter = ArgumentCaptor.forClass(Bson.class);
        ArgumentCaptor<Bson> update = ArgumentCaptor.forClass(Bson.class);
        verify(collection).updateOne(filter.capture(), update.capture(), any());
        assertTrue(filter.getValue().toBsonDocument().toJson().contains("item:1001"));
        String updateJson = update.getValue().toBsonDocument().toJson();
        assertTrue(updateJson.contains("金发·因陀罗"));
        assertTrue(updateJson.contains("updatedAt"));
        assertTrue(updateJson.contains("createdAt"));
    }

    @Test
    void shouldReadPayloadWithoutStorageMetadata() {
        MongoCollection<Document> collection = collection("jx3_price_snapshot");
        FindIterable<Document> iterable = mock(FindIterable.class);
        Document payload = new Document("name", "金发·因陀罗").append("price", 4300);
        when(collection.find(any(Bson.class))).thenReturn(iterable);
        when(iterable.projection(any(Bson.class))).thenReturn(iterable);
        when(iterable.first()).thenReturn(new Document("payload", payload));

        Optional<JsonNode> result = store.find("jx3_price_snapshot", "item:1001");

        assertTrue(result.isPresent());
        assertEquals("金发·因陀罗", result.orElseThrow().path("name").asText());
        assertEquals(4300, result.orElseThrow().path("price").asInt());
        assertFalse(result.orElseThrow().has("_id"));
    }

    @Test
    void shouldRejectUnsafeCollectionNameAndOversizedPayload() {
        JsonNode payload = objectMapper.createObjectNode().put("value", "ok");
        assertThrows(IllegalArgumentException.class,
                () -> store.upsert("system.users", "id", payload));
        assertThrows(IllegalArgumentException.class,
                () -> store.upsert("../users", "id", payload));

        properties.setMaxDocumentBytes(1024);
        JsonNode oversized = objectMapper.createObjectNode().put("value", "x".repeat(2048));
        assertThrows(IllegalArgumentException.class,
                () -> store.upsert("safe_collection", "id", oversized));
    }

    @Test
    void shouldRequireJsonObjectPayload() {
        assertThrows(IllegalArgumentException.class,
                () -> store.upsert("safe_collection", "id", objectMapper.createArrayNode().add("value")));
    }

    @SuppressWarnings("unchecked")
    private MongoCollection<Document> collection(String name) {
        MongoCollection<Document> collection = mock(MongoCollection.class);
        when(mongoTemplate.getCollection(name)).thenReturn(collection);
        return collection;
    }
}