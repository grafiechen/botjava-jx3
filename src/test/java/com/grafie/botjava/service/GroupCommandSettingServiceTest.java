package com.grafie.botjava.service;

import com.grafie.botjava.entity.GroupCommandSetting;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.mapper.GroupCommandSettingMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroupCommandSettingServiceTest {

    @Test
    void shouldReadKnownOverridesAndIgnoreStaleCommandNames() {
        GroupCommandSettingMapper mapper = mock(GroupCommandSettingMapper.class);
        when(mapper.findByGroupOpenIdOrderByCommandNameAsc("group-1")).thenReturn(List.of(
                setting("ServerCheck", false), setting("REMOVED_COMMAND", false)));

        Map<REGEX, Boolean> overrides = new GroupCommandSettingService(mapper).findOverrides("group-1");

        assertEquals(Map.of(REGEX.ServerCheck, false), overrides);
    }

    @Test
    void shouldCreateStableEnumKeyAndUpdateExistingSetting() {
        GroupCommandSettingMapper mapper = mock(GroupCommandSettingMapper.class);
        when(mapper.save(any(GroupCommandSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));
        GroupCommandSettingService service = new GroupCommandSettingService(mapper);

        GroupCommandSetting created = service.setEnabled("group-1", REGEX.ServerCheck, false);
        assertEquals("ServerCheck", created.getCommandName());
        assertEquals(false, created.getEnabled());

        GroupCommandSetting existing = setting("ServerCheck", false);
        when(mapper.findByGroupOpenIdAndCommandName("group-1", "ServerCheck")).thenReturn(existing);
        service.setEnabled("group-1", REGEX.ServerCheck, true);
        verify(mapper).save(existing);
        assertEquals(true, existing.getEnabled());
    }

    @Test
    void shouldRejectSystemCommandsAndMissingGroupId() {
        GroupCommandSettingService service = new GroupCommandSettingService(mock(GroupCommandSettingMapper.class));

        assertThrows(IllegalArgumentException.class,
                () -> service.setEnabled("group-1", REGEX.Help, false));
        assertThrows(IllegalArgumentException.class,
                () -> service.setEnabled(" ", REGEX.ServerCheck, false));
    }

    private static GroupCommandSetting setting(String commandName, boolean enabled) {
        GroupCommandSetting setting = new GroupCommandSetting();
        setting.setGroupOpenId("group-1");
        setting.setCommandName(commandName);
        setting.setEnabled(enabled);
        return setting;
    }
}
