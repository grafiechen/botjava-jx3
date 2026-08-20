package com.grafie.botjava.service;

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
    void shouldRejectQueryAndUpdateForRoleNotBoundToSender() {
        Fixture fixture = fixture(false);

        BotResponse query = fixture.service.query(fixture.message, "乾坤一掷", "别人");
        BotResponse update = fixture.service.update(fixture.message, "乾坤一掷", "别人", "秘籍", "完成");

        assertTrue(query.getContent().contains("只能查询自己已绑定"));
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
        ScriptStatusService service = new ScriptStatusService(provider, fieldService, preferenceService, auditService);
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