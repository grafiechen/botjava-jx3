package com.grafie.botjava.service;

import com.grafie.botjava.entity.GroupInfo;
import com.grafie.botjava.mapper.GroupInfoMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroupConfigurationServiceTest {

    @Test
    void shouldReadTrimmedWholeGroupServer() {
        GroupInfoMapper mapper = mock(GroupInfoMapper.class);
        GroupInfo groupInfo = new GroupInfo();
        groupInfo.setServer(" 乾坤一掷 ");
        when(mapper.findByOpenGroupId("group-1")).thenReturn(groupInfo);
        GroupConfigurationService service = new GroupConfigurationService(mapper);

        assertEquals(Optional.of("乾坤一掷"), service.findServer("group-1"));
        assertTrue(service.findServer(" ").isEmpty());
    }

    @Test
    void shouldCreateWholeGroupServerBindingAndKeepExistingBinding() {
        GroupInfoMapper mapper = mock(GroupInfoMapper.class);
        GroupConfigurationService service = new GroupConfigurationService(mapper);

        GroupConfigurationService.BindServerResult created = service.bindServer("group-1", " 乾坤一掷 ");

        ArgumentCaptor<GroupInfo> saved = ArgumentCaptor.forClass(GroupInfo.class);
        verify(mapper).saveAndFlush(saved.capture());
        assertEquals("group-1", saved.getValue().getOpenGroupId());
        assertEquals("乾坤一掷", saved.getValue().getServer());
        assertTrue(created.bound());

        GroupInfo existing = new GroupInfo();
        existing.setServer("梦江南");
        when(mapper.findByOpenGroupId("group-2")).thenReturn(existing);
        GroupConfigurationService.BindServerResult rejected = service.bindServer("group-2", "乾坤一掷");
        assertFalse(rejected.bound());
        assertEquals("梦江南", rejected.server());
    }

    @Test
    void shouldRecoverFromConcurrentFirstInsert() {
        GroupInfoMapper mapper = mock(GroupInfoMapper.class);
        when(mapper.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("unique race"));
        when(mapper.bindServerIfEmpty("group-1", "乾坤一掷")).thenReturn(0, 1);
        GroupConfigurationService service = new GroupConfigurationService(mapper);

        GroupConfigurationService.BindServerResult result = service.bindServer("group-1", "乾坤一掷");

        assertTrue(result.bound());
        verify(mapper, org.mockito.Mockito.times(2)).bindServerIfEmpty("group-1", "乾坤一掷");
    }

    @Test
    void shouldUpdateExistingSettingWithoutCreatingAnotherGroup() {
        GroupInfoMapper mapper = mock(GroupInfoMapper.class);
        when(mapper.updateExperimentalEnabled("group-1", true)).thenReturn(1);
        GroupConfigurationService service = new GroupConfigurationService(mapper);

        service.setEnabled("group-1", GroupConfigurationService.Setting.EXPERIMENTAL, true);

        verify(mapper).updateExperimentalEnabled("group-1", true);
        verify(mapper, never()).saveAndFlush(any());
    }

    @Test
    void shouldCreateGroupWithRequestedSetting() {
        GroupInfoMapper mapper = mock(GroupInfoMapper.class);
        GroupConfigurationService service = new GroupConfigurationService(mapper);

        service.setEnabled("group-1", GroupConfigurationService.Setting.ACTIVE_MESSAGES, true);

        ArgumentCaptor<GroupInfo> saved = ArgumentCaptor.forClass(GroupInfo.class);
        verify(mapper).saveAndFlush(saved.capture());
        assertEquals("group-1", saved.getValue().getOpenGroupId());
        assertTrue(saved.getValue().getActiveMessagesEnabled());
    }

    @Test
    void shouldKeepPlatformAuthorizationSeparateFromLocalSwitch() {
        GroupInfoMapper mapper = mock(GroupInfoMapper.class);
        GroupConfigurationService service = new GroupConfigurationService(mapper);

        service.setActiveMessagesPlatformAllowed("group-1", true);

        ArgumentCaptor<GroupInfo> saved = ArgumentCaptor.forClass(GroupInfo.class);
        verify(mapper).saveAndFlush(saved.capture());
        assertEquals("group-1", saved.getValue().getOpenGroupId());
        assertTrue(saved.getValue().getActiveMessagesPlatformAllowed());
        assertEquals(null, saved.getValue().getActiveMessagesEnabled());
        verify(mapper, never()).updateActiveMessagesEnabled("group-1", true);
    }
}
