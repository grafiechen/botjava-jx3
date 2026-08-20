package com.grafie.botjava.service;

import com.grafie.botjava.entity.GroupDailyPushField;
import com.grafie.botjava.entity.ScriptStatusFieldDefinition;
import com.grafie.botjava.mapper.GroupDailyPushFieldMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GroupDailyPushFieldServiceTest {
    @Test
    void shouldUseRealDumpFieldsAsDefaultsUntilGroupCreatesConfiguration() {
        GroupDailyPushFieldMapper mapper = mock(GroupDailyPushFieldMapper.class);
        when(mapper.existsByGroupOpenId("group-1")).thenReturn(false);
        when(mapper.findByGroupOpenIdAndEnabledTrueOrderBySortOrderAscIdAsc("group-1")).thenReturn(List.of());
        GroupDailyPushFieldService service = new GroupDailyPushFieldService(mapper);

        assertThat(service.enabledFields("group-1"))
                .extracting(GroupDailyPushField::getMongoFieldName)
                .containsExactly("每日签到任务", "角色金币", "精力", "侠行点");
    }

    @Test
    void groupOverrideShouldRemoveOneDefaultAndKeepTheOthers() {
        GroupDailyPushFieldMapper mapper = mock(GroupDailyPushFieldMapper.class);
        GroupDailyPushField disabled = new GroupDailyPushField();
        disabled.setMongoFieldName("精力");
        disabled.setEnabled(false);
        when(mapper.findByGroupOpenIdOrderBySortOrderAscIdAsc("group-1")).thenReturn(List.of(disabled));

        assertThat(new GroupDailyPushFieldService(mapper).enabledFields("group-1"))
                .extracting(GroupDailyPushField::getMongoFieldName)
                .containsExactly("每日签到任务", "角色金币", "侠行点");
    }

    @Test
    void shouldPersistTombstoneWhenDeletingDefaultField() {
        GroupDailyPushFieldMapper mapper = mock(GroupDailyPushFieldMapper.class);
        GroupDailyPushFieldService service = new GroupDailyPushFieldService(mapper);

        assertThat(service.disable("group-1", "精力", "member-1")).isTrue();
        verify(mapper).save(argThat(field -> "精力".equals(field.getMongoFieldName()) && !field.isEnabled()));
    }
    @Test
    void shouldRejectIdentityAndSensitiveMongoFields() {
        GroupDailyPushFieldService service = new GroupDailyPushFieldService(mock(GroupDailyPushFieldMapper.class));
        assertThatThrownBy(() -> service.save("group-1", "账号", "账号",
                ScriptStatusFieldDefinition.ValueType.TEXT.name(), 1, "member-1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.save("group-1", "$where", "危险字段",
                ScriptStatusFieldDefinition.ValueType.TEXT.name(), 1, "member-1"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}