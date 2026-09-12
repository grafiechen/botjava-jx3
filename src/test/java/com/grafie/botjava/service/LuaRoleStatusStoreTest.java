package com.grafie.botjava.service;

import com.grafie.botjava.config.BotMongoProperties;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LuaRoleStatusStoreTest {

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnAllMatchesAndUpdateOnlyTheirExistingIds() {
        MongoTemplate template = mock(MongoTemplate.class);
        MongoCollection<Document> collection = mock(MongoCollection.class);
        FindIterable<Document> iterable = mock(FindIterable.class);
        when(template.getCollection("roles")).thenReturn(collection);
        when(collection.find(any(Bson.class))).thenReturn(iterable);
        when(iterable.limit(21)).thenReturn(iterable);
        List<Document> source = List.of(
                new Document("_id", "one").append("服务器", "乾坤一掷").append("角色名", "加菲"),
                new Document("_id", "two").append("服务器", "乾坤一掷").append("角色名", "加菲")
        );
        when(iterable.into(any(Collection.class))).thenAnswer(invocation -> {
            Collection<Document> target = invocation.getArgument(0);
            target.addAll(source);
            return target;
        });
        UpdateResult updateResult = mock(UpdateResult.class);
        when(updateResult.getMatchedCount()).thenReturn(2L);
        when(updateResult.getModifiedCount()).thenReturn(2L);
        when(collection.updateMany(any(Bson.class), any(Bson.class))).thenReturn(updateResult);
        LuaRoleStatusStore store = new LuaRoleStatusStore(template, new BotMongoProperties());

        List<Document> found = store.findByServerAndRoleName("乾坤一掷", "加菲");
        LuaRoleStatusStore.UpdateSummary updated = store.updateByServerAndRoleName(
                "乾坤一掷", "加菲", "秘籍", true);

        assertEquals(2, found.size());
        assertEquals(2, updated.matchedCount());
        verify(collection).updateMany(any(Bson.class), any(Bson.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReadOnlyRoleAndBagFieldsFromEveryDocument() {
        MongoTemplate template = mock(MongoTemplate.class);
        MongoCollection<Document> collection = mock(MongoCollection.class);
        FindIterable<Document> iterable = mock(FindIterable.class);
        when(template.getCollection("roles")).thenReturn(collection);
        when(collection.find()).thenReturn(iterable);
        when(iterable.projection(any(Bson.class))).thenReturn(iterable);
        when(iterable.into(any(Collection.class))).thenAnswer(invocation -> {
            Collection<Document> target = invocation.getArgument(0);
            target.addAll(List.of(
                    new Document("服务器", "乾坤一掷").append("角色名", "加菲").append("背包剩余空间", 12),
                    new Document("服务器", "梦江南").append("角色名", "琉枫").append("背包剩余空间", "49")
            ));
            return target;
        });
        LuaRoleStatusStore store = new LuaRoleStatusStore(template, new BotMongoProperties());

        List<LuaRoleStatusStore.RoleBagSpaceRecord> records = store.findAllRoleBagSpaces();

        assertEquals(2, records.size());
        assertEquals("乾坤一掷", records.getFirst().server());
        assertEquals(12, records.getFirst().remainingSpace());
        verify(collection).find();
        verify(iterable).projection(any(Bson.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReadOnlyIdentityAndRequestedFieldsFromMatchingDocuments() {
        MongoTemplate template = mock(MongoTemplate.class);
        MongoCollection<Document> collection = mock(MongoCollection.class);
        FindIterable<Document> iterable = mock(FindIterable.class);
        when(template.getCollection("roles")).thenReturn(collection);
        when(collection.find(any(Bson.class))).thenReturn(iterable);
        when(iterable.projection(any(Bson.class))).thenReturn(iterable);
        when(iterable.into(any(Collection.class))).thenAnswer(invocation -> {
            Collection<Document> target = invocation.getArgument(0);
            target.addAll(List.of(
                    new Document("服务器", "乾坤一掷").append("角色名", "加菲").append("侠行点", 1200),
                    new Document("服务器", "梦江南").append("角色名", "琉枫").append("侠行点", 3600)
            ));
            return target;
        });
        LuaRoleStatusStore store = new LuaRoleStatusStore(template, new BotMongoProperties());

        List<LuaRoleStatusStore.RoleFieldRecord> records = store.findAllRoleFields(List.of("侠行点"));

        assertEquals(2, records.size());
        assertEquals("加菲", records.getFirst().roleName());
        assertEquals(java.util.Map.of("侠行点", 1200), records.getFirst().values());
        verify(collection).find(any(Bson.class));
        verify(iterable).projection(any(Bson.class));
    }
    @Test
    @SuppressWarnings("unchecked")
    void shouldRejectMoreMatchesThanConfiguredLimit() {
        MongoTemplate template = mock(MongoTemplate.class);
        MongoCollection<Document> collection = mock(MongoCollection.class);
        FindIterable<Document> iterable = mock(FindIterable.class);
        when(template.getCollection("roles")).thenReturn(collection);
        when(collection.find(any(Bson.class))).thenReturn(iterable);
        when(iterable.limit(3)).thenReturn(iterable);
        when(iterable.into(any(Collection.class))).thenAnswer(invocation -> {
            Collection<Document> target = invocation.getArgument(0);
            target.addAll(List.of(new Document("_id", 1), new Document("_id", 2), new Document("_id", 3)));
            return target;
        });
        BotMongoProperties properties = new BotMongoProperties();
        properties.getRoleStatus().setMaxMatches(2);
        LuaRoleStatusStore store = new LuaRoleStatusStore(template, properties);

        LuaRoleStatusStore.TooManyMatchesException exception = assertThrows(
                LuaRoleStatusStore.TooManyMatchesException.class,
                () -> store.findByServerAndRoleName("乾坤一掷", "加菲"));

        assertEquals(2, exception.getMaximum());
        assertTrue(exception.getMessage().contains("2"));
    }
}