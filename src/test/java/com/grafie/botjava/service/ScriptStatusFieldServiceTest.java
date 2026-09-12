package com.grafie.botjava.service;

import com.grafie.botjava.config.BotMongoProperties;
import com.grafie.botjava.entity.ScriptStatusFieldDefinition;
import com.grafie.botjava.mapper.ScriptStatusFieldDefinitionMapper;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

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
    void shouldResolveBuiltInRoleInfoWithoutDatabaseConfiguration() {
        ScriptStatusFieldDefinitionMapper mapper = mock(ScriptStatusFieldDefinitionMapper.class);
        ScriptStatusFieldService service = new ScriptStatusFieldService(mapper, new BotMongoProperties());

        List<ScriptStatusFieldService.ResolvedQueryField> resolved = service.resolveQueryFields("角色信息");

        assertEquals(List.of(
                new ScriptStatusFieldService.ResolvedQueryField("背包剩余空间", "背包剩余空间"),
                new ScriptStatusFieldService.ResolvedQueryField("角色金币", "金币"),
                new ScriptStatusFieldService.ResolvedQueryField("精力", "精力"),
                new ScriptStatusFieldService.ResolvedQueryField("侠行点", "侠义点"),
                new ScriptStatusFieldService.ResolvedQueryField("威望", "威望")
        ), resolved);
    }

    @Test
    void shouldResolveExactFieldBeforeDisplayNameAlias() {
        ScriptStatusFieldDefinitionMapper mapper = mock(ScriptStatusFieldDefinitionMapper.class);
        when(mapper.findByEnabledTrueOrderByGroupNameAscSortOrderAscIdAsc()).thenReturn(List.of(
                field("角色金币", "金币", "货币"), field("侠行点", "侠义", "货币"),
                field("货币", "货币汇总", "汇总")
        ));
        ScriptStatusFieldService service = new ScriptStatusFieldService(mapper, new BotMongoProperties());

        List<ScriptStatusFieldService.ResolvedQueryField> resolved = service.resolveQueryFields("货币");

        assertEquals(1, resolved.size());
        assertEquals("货币", resolved.getFirst().mongoFieldName());
    }

    @Test
    void shouldExpandGroupAliasAndNormalizeAliasInput() {
        ScriptStatusFieldDefinitionMapper mapper = mock(ScriptStatusFieldDefinitionMapper.class);
        when(mapper.findByEnabledTrueOrderByGroupNameAscSortOrderAscIdAsc()).thenReturn(List.of(
                field("角色金币", "金币", "货币"), field("侠行点", "侠义", "货币")
        ));
        ScriptStatusFieldService service = new ScriptStatusFieldService(mapper, new BotMongoProperties());

        List<ScriptStatusFieldService.ResolvedQueryField> aliasFields = service.resolveQueryFields("货-币");
        List<ScriptStatusFieldService.ResolvedQueryField> rawFields = service.resolveQueryFields("精力");

        assertEquals(List.of("角色金币", "侠行点"), aliasFields.stream()
                .map(ScriptStatusFieldService.ResolvedQueryField::mongoFieldName).toList());
        assertEquals(List.of("精力"), rawFields.stream()
                .map(ScriptStatusFieldService.ResolvedQueryField::mongoFieldName).toList());
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
    private ScriptStatusFieldDefinition field(String mongoFieldName, String displayName, String groupName) {
        ScriptStatusFieldDefinition definition = new ScriptStatusFieldDefinition();
        definition.setMongoFieldName(mongoFieldName);
        definition.setDisplayName(displayName);
        definition.setGroupName(groupName);
        definition.setEnabled(true);
        return definition;
    }
}
