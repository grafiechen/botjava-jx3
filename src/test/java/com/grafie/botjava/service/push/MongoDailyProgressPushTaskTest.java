package com.grafie.botjava.service.push;

import com.grafie.botjava.entity.GroupDailyPushField;
import com.grafie.botjava.entity.ScriptStatusFieldDefinition;
import com.grafie.botjava.entity.UserRoleBinding;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.service.*;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MongoDailyProgressPushTaskTest {
    @Test
    void shouldSkipSubscribedGroupWithoutBindings() {
        Fixture fixture = fixture();
        when(fixture.preferences.findGroupBindings("group-1")).thenReturn(List.of());

        assertThat(fixture.task.buildResponse("group-1")).isNull();
        verifyNoInteractions(fixture.store);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldRenderAllDuplicateMongoDocumentsAndConvertUnixSeconds() {
        Fixture fixture = fixture();
        UserRoleBinding binding = new UserRoleBinding();
        binding.setGroupOpenId("group-1");
        binding.setMemberOpenId("member-1");
        binding.setServer("乾坤一掷");
        binding.setRoleName("醉卧平沙");
        GroupDailyPushField completed = field("每日签到任务", "上次日常完成",
                ScriptStatusFieldDefinition.ValueType.DATETIME, 10);
        GroupDailyPushField gold = field("角色金币", "金币",
                ScriptStatusFieldDefinition.ValueType.INTEGER, 20);
        when(fixture.preferences.findGroupBindings("group-1")).thenReturn(List.of(binding, binding));
        when(fixture.fields.enabledFields("group-1")).thenReturn(List.of(completed, gold));
        when(fixture.store.findByServerAndRoleName("乾坤一掷", "醉卧平沙")).thenReturn(List.of(
                new Document(Map.of("每日签到任务", 1786435364, "角色金币", 27655)),
                new Document(Map.of("每日签到任务", 1786448145, "角色金币", 30000))));

        BotResponse response = fixture.task.buildResponse("group-1");

        assertThat(response.getResponseType()).isEqualTo(BotResponse.ResponseType.IMAGE);
        assertThat(response.getTemplateName()).isEqualTo("日常进度");
        Map<String, Object> data = (Map<String, Object>) response.getTemplateData();
        List<Map<String, Object>> rows = (List<Map<String, Object>>) data.get("rows");
        assertThat(rows).hasSize(2);
        assertThat((List<String>) rows.getFirst().get("values"))
                .containsExactly("2026-08-11 17:02:44", "27655");
        verify(fixture.store, times(1)).findByServerAndRoleName("乾坤一掷", "醉卧平沙");
    }

    private static GroupDailyPushField field(String mongo, String display,
                                             ScriptStatusFieldDefinition.ValueType type, int order) {
        GroupDailyPushField field = new GroupDailyPushField();
        field.setMongoFieldName(mongo);
        field.setDisplayName(display);
        field.setValueType(type);
        field.setSortOrder(order);
        field.setEnabled(true);
        return field;
    }

    private static Fixture fixture() {
        PushTaskRegistry registry = new PushTaskRegistry();
        ScheduledGroupPushRunner runner = mock(ScheduledGroupPushRunner.class);
        UserCommandPreferenceService preferences = mock(UserCommandPreferenceService.class);
        GroupDailyPushFieldService fields = mock(GroupDailyPushFieldService.class);
        LuaRoleStatusStore store = mock(LuaRoleStatusStore.class);
        MongoDisplayValueFormatter formatter = new MongoDisplayValueFormatter("Asia/Tokyo");
        MongoDailyProgressPushTask task = new MongoDailyProgressPushTask(
                registry, runner, preferences, fields, store, formatter, "Asia/Tokyo");
        return new Fixture(task, preferences, fields, store);
    }

    private record Fixture(MongoDailyProgressPushTask task,
                           UserCommandPreferenceService preferences,
                           GroupDailyPushFieldService fields,
                           LuaRoleStatusStore store) { }
}