package com.grafie.botjava.service;

import com.grafie.botjava.config.BotMongoProperties;
import com.grafie.botjava.entity.ScriptStatusFieldDefinition;
import com.grafie.botjava.mapper.ScriptStatusFieldDefinitionMapper;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScriptStatusFieldServiceTest {

    @Test
    void shouldCreateWritableTypedFieldAndParseValues() {
        ScriptStatusFieldDefinitionMapper mapper = mock(ScriptStatusFieldDefinitionMapper.class);
        when(mapper.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ScriptStatusFieldService service = new ScriptStatusFieldService(mapper, new BotMongoProperties());

        ScriptStatusFieldDefinition booleanField = service.save(
                "秘籍", "秘籍完成", "日常", "BOOLEAN", "WRITE", 10, "master");
        ScriptStatusFieldDefinition dateField = service.save(
                "更新时间", "更新时间", "基础", "DATETIME", "READ", 1, "master");

        assertTrue(booleanField.isWritable());
        assertEquals(Boolean.TRUE, service.parseValue(booleanField, "完成"));
        assertInstanceOf(Date.class, service.parseValue(dateField, "2026-08-12T21:30:00"));
    }

    @Test
    void shouldRejectProtectedOrNestedMongoField() {
        ScriptStatusFieldService service = new ScriptStatusFieldService(
                mock(ScriptStatusFieldDefinitionMapper.class), new BotMongoProperties());

        assertThrows(IllegalArgumentException.class,
                () -> service.save("全局ID", "全局", "基础", "TEXT", "READ", 1, "master"));
        assertThrows(IllegalArgumentException.class,
                () -> service.save("config.value", "配置", "基础", "TEXT", "WRITE", 1, "master"));
        assertThrows(IllegalArgumentException.class,
                () -> service.save("服务器", "服务器", "基础", "TEXT", "WRITE", 1, "master"));
        assertThrows(IllegalArgumentException.class,
                () -> service.save("角色名", "角色", "基础", "TEXT", "WRITE", 1, "master"));
    }
}