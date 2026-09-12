package com.grafie.botjava.service;

import com.grafie.botjava.config.BotMongoProperties;
import com.grafie.botjava.entity.ScriptStatusFieldDefinition;
import com.grafie.botjava.entity.UserRoleBinding;
import com.grafie.botjava.entity.dto.common.AuthorDto;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScriptStatusServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void shouldRenderEveryMongoMatchForOwnedRole() {
        Fixture fixture = fixture(true);
        when(fixture.store.findByServerAndRoleName("乾坤一掷", "加菲")).thenReturn(List.of(
                new Document("秘籍", true).append("角色金币", 1000),
                new Document("秘籍", false).append("角色金币", 2000)
        ));

        BotResponse response = fixture.service.query(fixture.message, "乾坤一掷", "加菲");

        assertEquals(BotResponse.ResponseType.IMAGE, response.getResponseType());
        assertEquals("脚本状态", response.getTemplateName());
        Map<String, Object> data = (Map<String, Object>) response.getTemplateData();
        assertEquals(2, data.get("matchCount"));
        assertEquals(2, ((List<?>) data.get("records")).size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldNormalizeScalarAndCollectionFieldValuesAsImageRows() {
        Fixture fixture = fixture(true);
        when(fixture.store.findByServerAndRoleName("乾坤一掷", "加菲")).thenReturn(List.of(
                new Document("侠行点", List.of("第一条", "第二条")),
                new Document("侠行点", 3000)
        ));

        BotResponse response = fixture.service.queryField(
                fixture.message, "乾坤一掷", "加菲", "侠行点");

        assertEquals(BotResponse.ResponseType.IMAGE, response.getResponseType());
        assertEquals("查询信息", response.getTemplateName());
        Map<String, Object> data = (Map<String, Object>) response.getTemplateData();
        assertEquals("侠行点", data.get("queryName"));
        assertEquals(1, data.get("fieldCount"));
        assertEquals(1, data.get("visibleFieldCount"));
        assertEquals(3, data.get("itemCount"));
        assertEquals(2, data.get("recordCount"));
        assertEquals(2, ((List<?>) data.get("records")).size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldExpandOneAliasToEveryConfiguredMongoField() {
        Fixture fixture = fixture(true);
        when(fixture.fieldService.resolveQueryFields("货币")).thenReturn(List.of(
                new ScriptStatusFieldService.ResolvedQueryField("角色金币", "货币"),
                new ScriptStatusFieldService.ResolvedQueryField("侠行点", "货币")));
        when(fixture.store.findByServerAndRoleName("乾坤一掷", "加菲")).thenReturn(List.of(
                new Document("角色金币", 1200).append("侠行点", 3600)
        ));

        BotResponse response = fixture.service.queryField(
                fixture.message, "乾坤一掷", "加菲", "货币");

        assertEquals(BotResponse.ResponseType.IMAGE, response.getResponseType());
        Map<String, Object> data = (Map<String, Object>) response.getTemplateData();
        assertEquals("货币", data.get("queryName"));
        assertEquals(2, data.get("fieldCount"));
        assertEquals(2, data.get("visibleFieldCount"));
        assertEquals(2, data.get("itemCount"));
        List<Map<String, Object>> records = (List<Map<String, Object>>) data.get("records");
        assertEquals(2, ((List<?>) records.getFirst().get("fields")).size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldRenderRequestedFieldForEveryMongoRoleWithoutBinding() {
        Fixture fixture = fixture(false);
        when(fixture.store.findAllRoleFields(List.of("侠行点"))).thenReturn(List.of(
                new LuaRoleStatusStore.RoleFieldRecord(
                        "梦江南", "琉枫", Map.of("侠行点", 3600)),
                new LuaRoleStatusStore.RoleFieldRecord(
                        "乾坤一掷", "加菲", Map.of("侠行点", List.of("1200", "2400"))),
                new LuaRoleStatusStore.RoleFieldRecord(
                        "唯我独尊", "空值", java.util.Collections.singletonMap("侠行点", null))
        ));

        BotResponse response = fixture.service.queryAllFields("侠行点");

        assertEquals(BotResponse.ResponseType.IMAGE, response.getResponseType());
        assertEquals("查询全部信息", response.getTemplateName());
        Map<String, Object> data = (Map<String, Object>) response.getTemplateData();
        assertEquals("侠行点", data.get("queryName"));
        assertEquals(2, data.get("recordCount"));
        assertEquals(3, data.get("itemCount"));
        List<Map<String, Object>> records = (List<Map<String, Object>>) data.get("records");
        assertEquals("加菲", records.getFirst().get("roleName"));
        assertEquals("琉枫", records.get(1).get("roleName"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldMarkOwnedValuesForAllRoleFieldTemplate() {
        Fixture fixture = fixture(false);
        when(fixture.store.findAllRoleFields(List.of("周年挂件"))).thenReturn(List.of(
                new LuaRoleStatusStore.RoleFieldRecord(
                        "乾坤一掷", "已拥有角色", Map.of("周年挂件", true)),
                new LuaRoleStatusStore.RoleFieldRecord(
                        "乾坤一掷", "未拥有角色", Map.of("周年挂件", false))));

        BotResponse response = fixture.service.queryAllFields("周年挂件");

        Map<String, Object> data = (Map<String, Object>) response.getTemplateData();
        assertEquals(true, data.get("hasOwnedValues"));
        List<Map<String, Object>> records = (List<Map<String, Object>>) data.get("records");
        List<Map<String, Object>> ownedFields = (List<Map<String, Object>>) records.getFirst().get("fields");
        List<Map<String, Object>> ownedItems = (List<Map<String, Object>>) ownedFields.getFirst().get("items");
        List<Map<String, Object>> missingFields = (List<Map<String, Object>>) records.get(1).get("fields");
        List<Map<String, Object>> missingItems = (List<Map<String, Object>>) missingFields.getFirst().get("items");
        assertEquals(true, ownedItems.getFirst().get("owned"));
        assertEquals(false, missingItems.getFirst().get("owned"));
        assertEquals("否", missingItems.getFirst().get("value"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEveryAllRoleFieldValueBeyondSingleRoleLimit() {
        Fixture fixture = fixture(false);
        List<LuaRoleStatusStore.RoleFieldRecord> documents = java.util.stream.IntStream.range(0, 201)
                .mapToObj(index -> new LuaRoleStatusStore.RoleFieldRecord(
                        "乾坤一掷", "角色" + index, Map.of("侠行点", index)))
                .toList();
        when(fixture.store.findAllRoleFields(List.of("侠行点"))).thenReturn(documents);

        BotResponse response = fixture.service.queryAllFields("侠行点");

        Map<String, Object> data = (Map<String, Object>) response.getTemplateData();
        assertEquals(201, data.get("recordCount"));
        assertEquals(201, data.get("itemCount"));
        assertEquals(false, data.get("truncated"));
    }
    @Test
    void shouldReturnTextWhenAllRoleFieldDoesNotExistOrHasNoValue() {
        Fixture fixture = fixture(false);
        when(fixture.store.findAllRoleFields(List.of("不存在字段"))).thenReturn(List.of());

        BotResponse response = fixture.service.queryAllFields("不存在字段");

        assertEquals(BotResponse.ResponseType.TEXT, response.getResponseType());
        assertEquals("未查询到“不存在字段”的数据。", response.getContent());
    }

    @Test
    void shouldRejectSensitiveAllRoleFieldBeforeReadingMongo() {
        Fixture fixture = fixture(false);

        BotResponse response = fixture.service.queryAllFields("账号");

        assertTrue(response.getContent().contains("敏感信息"));
        verify(fixture.store, never()).findAllRoleFields(any());
    }
    @Test
    @SuppressWarnings("unchecked")
    void shouldRenderPublicLowBagSpaceRecordsWithoutRoleBinding() {
        Fixture fixture = fixture(false);
        when(fixture.store.findAllRoleBagSpaces()).thenReturn(List.of(
                new LuaRoleStatusStore.RoleBagSpaceRecord("乾坤一掷", "加菲", 49),
                new LuaRoleStatusStore.RoleBagSpaceRecord("梦江南", "琉枫", "8"),
                new LuaRoleStatusStore.RoleBagSpaceRecord("唯我独尊", "满背包", 50),
                new LuaRoleStatusStore.RoleBagSpaceRecord("长安城", "未上报", "未知")
        ));

        BotResponse response = fixture.service.queryBagSpaceWarning();

        assertEquals(BotResponse.ResponseType.IMAGE, response.getResponseType());
        assertEquals("背包预警", response.getTemplateName());
        Map<String, Object> data = (Map<String, Object>) response.getTemplateData();
        assertEquals(50, data.get("threshold"));
        assertEquals(2, data.get("count"));
        List<Map<String, Object>> records = (List<Map<String, Object>>) data.get("records");
        assertEquals("琉枫", records.getFirst().get("roleName"));
        assertEquals("8", records.getFirst().get("remainingSpace"));
        assertEquals("加菲", records.get(1).get("roleName"));
    }

    @Test
    void shouldReturnTextWhenNoRoleHasLowBagSpace() {
        Fixture fixture = fixture(false);
        when(fixture.store.findAllRoleBagSpaces()).thenReturn(List.of(
                new LuaRoleStatusStore.RoleBagSpaceRecord("乾坤一掷", "加菲", 50),
                new LuaRoleStatusStore.RoleBagSpaceRecord("梦江南", "琉枫", null)
        ));

        BotResponse response = fixture.service.queryBagSpaceWarning();

        assertEquals(BotResponse.ResponseType.TEXT, response.getResponseType());
        assertTrue(response.getContent().contains("当前没有"));
        assertNull(fixture.service.buildBagSpaceWarningPush());
    }

    @Test
    void shouldRejectSensitiveFieldAfterAliasExpansion() {
        Fixture fixture = fixture(true);
        when(fixture.fieldService.resolveQueryFields("身份")).thenReturn(List.of(
                new ScriptStatusFieldService.ResolvedQueryField("全局ID", "身份")));

        BotResponse response = fixture.service.queryField(
                fixture.message, "乾坤一掷", "加菲", "身份");

        assertTrue(response.getContent().contains("敏感信息"));
        verify(fixture.store, never()).findByServerAndRoleName(any(), any());
    }

    @Test
    void shouldReturnNoDataForMissingBlankAndEmptyValues() {
        Fixture fixture = fixture(true);
        when(fixture.store.findByServerAndRoleName("乾坤一掷", "加菲")).thenReturn(List.of(
                new Document("侠行点", null),
                new Document("侠行点", "   "),
                new Document("侠行点", List.of())
        ));

        BotResponse response = fixture.service.queryField(
                fixture.message, "乾坤一掷", "加菲", "侠行点");

        assertEquals(BotResponse.ResponseType.TEXT, response.getResponseType());
        assertEquals("未查询到数据。", response.getContent());
    }

    @Test
    void shouldRejectSensitiveFieldWithoutReadingMongo() {
        Fixture fixture = fixture(true);

        BotResponse response = fixture.service.queryField(
                fixture.message, "乾坤一掷", "加菲", "全局ID");

        assertTrue(response.getContent().contains("敏感信息"));
        verify(fixture.store, never()).findByServerAndRoleName(any(), any());
    }

    @Test
    void shouldConvertMongoFailureToReadableResponse() {
        Fixture fixture = fixture(true);
        when(fixture.store.findByServerAndRoleName("乾坤一掷", "加菲"))
                .thenThrow(new RuntimeException("mongo unavailable"));

        BotResponse response = fixture.service.queryField(
                fixture.message, "乾坤一掷", "加菲", "侠行点");

        assertEquals(BotResponse.ResponseType.TEXT, response.getResponseType());
        assertTrue(response.getContent().contains("查询失败"));
    }

    @Test
    void shouldSupportConfiguredDenyListAndReservedWhitelist() {
        BotMongoProperties properties = new BotMongoProperties();
        properties.getRoleFieldQuery().setDeniedFields(List.of("角色金币"));
        RoleFieldQueryAccessPolicy policy = new RoleFieldQueryAccessPolicy(properties);

        assertThrows(IllegalArgumentException.class, () -> policy.requireQueryableField("角色金币"));
        assertEquals("侠行点", policy.requireQueryableField("侠行点"));

        properties.getRoleFieldQuery().setWhitelistEnabled(true);
        properties.getRoleFieldQuery().setAllowedFields(List.of("侠行点"));
        assertEquals("侠行点", policy.requireQueryableField("侠行点"));
        assertThrows(IllegalArgumentException.class, () -> policy.requireQueryableField("精力"));
    }

    @Test
    void shouldRejectQueryAndUpdateForRoleNotBoundToSender() {
        Fixture fixture = fixture(false);

        BotResponse query = fixture.service.query(fixture.message, "乾坤一掷", "别人");
        BotResponse fieldQuery = fixture.service.queryField(fixture.message, "乾坤一掷", "别人", "侠行点");
        BotResponse update = fixture.service.update(fixture.message, "乾坤一掷", "别人", "秘籍", "完成");

        assertTrue(query.getContent().contains("只能查询自己已绑定"));
        assertTrue(fieldQuery.getContent().contains("只能查询自己已绑定"));
        assertTrue(update.getContent().contains("只能修改自己已绑定"));
        verify(fixture.store, never()).findByServerAndRoleName(any(), any());
        verify(fixture.store, never()).updateByServerAndRoleName(any(), any(), any(), any());
    }

    @Test
    void shouldUpdateAllMatchesOnlyWhenFieldIsWritable() {
        Fixture fixture = fixture(true);
        ScriptStatusFieldDefinition writable = field("秘籍", "秘籍完成", true);
        when(fixture.fieldService.findEnabled("秘籍")).thenReturn(writable);
        when(fixture.fieldService.parseValue(writable, "完成")).thenReturn(true);
        when(fixture.store.updateByServerAndRoleName("乾坤一掷", "加菲", "秘籍", true))
                .thenReturn(new LuaRoleStatusStore.UpdateSummary(2, 2));

        BotResponse response = fixture.service.update(fixture.message, "乾坤一掷", "加菲", "秘籍", "完成");

        assertTrue(response.getContent().contains("影响 2 条"));
        verify(fixture.auditService).record(eq("SCRIPT_STATUS"), eq("UPDATE"), eq("秘籍"),
                eq("member-1"), eq("GROUP"), eq(true), eq(null));
    }

    @Test
    void shouldRejectReadOnlyField() {
        Fixture fixture = fixture(true);
        when(fixture.fieldService.findEnabled("角色金币")).thenReturn(field("角色金币", "金币", false));

        BotResponse response = fixture.service.update(
                fixture.message, "乾坤一掷", "加菲", "角色金币", "999999");

        assertTrue(response.getContent().contains("只读字段"));
        verify(fixture.store, never()).updateByServerAndRoleName(any(), any(), any(), any());
    }

    @SuppressWarnings("unchecked")
    private Fixture fixture(boolean ownsRole) {
        ObjectProvider<LuaRoleStatusStore> provider = mock(ObjectProvider.class);
        LuaRoleStatusStore store = mock(LuaRoleStatusStore.class);
        when(provider.getIfAvailable()).thenReturn(store);
        ScriptStatusFieldService fieldService = mock(ScriptStatusFieldService.class);
        when(fieldService.resolveQueryFields(any())).thenAnswer(invocation -> {
            String requested = invocation.getArgument(0);
            return List.of(new ScriptStatusFieldService.ResolvedQueryField(requested, requested));
        });
        UserCommandPreferenceService preferenceService = mock(UserCommandPreferenceService.class);
        BotAdminAuditService auditService = mock(BotAdminAuditService.class);
        GroupAtMessageCreateDto message = message();
        UserRoleBinding role = new UserRoleBinding();
        role.setServer("乾坤一掷");
        role.setRoleName(ownsRole ? "加菲" : "其他角色");
        when(preferenceService.findBindings(message)).thenReturn(
                new UserCommandPreferenceService.BindingSnapshot(null, List.of(role)));
        when(fieldService.enabledFields()).thenReturn(List.of(
                field("秘籍", "秘籍完成", true), field("角色金币", "金币", false)));
        BotMongoProperties properties = new BotMongoProperties();
        RoleFieldQueryAccessPolicy policy = new RoleFieldQueryAccessPolicy(properties);
        ScriptStatusService service = new ScriptStatusService(
                provider, fieldService, policy, preferenceService, auditService);
        return new Fixture(service, store, fieldService, auditService, message);
    }

    private ScriptStatusFieldDefinition field(String mongoName, String displayName, boolean writable) {
        ScriptStatusFieldDefinition field = new ScriptStatusFieldDefinition();
        field.setMongoFieldName(mongoName);
        field.setDisplayName(displayName);
        field.setGroupName("角色状态");
        field.setWritable(writable);
        field.setEnabled(true);
        return field;
    }

    private GroupAtMessageCreateDto message() {
        AuthorDto author = new AuthorDto();
        author.setMemberOpenid("member-1");
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setAuthor(author);
        return message;
    }

    private record Fixture(ScriptStatusService service, LuaRoleStatusStore store,
                           ScriptStatusFieldService fieldService, BotAdminAuditService auditService,
                           GroupAtMessageCreateDto message) {
    }
}
