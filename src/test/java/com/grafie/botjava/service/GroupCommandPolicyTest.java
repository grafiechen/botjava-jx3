package com.grafie.botjava.service;

import com.grafie.botjava.config.CommandReleaseProperties;
import com.grafie.botjava.entity.GroupInfo;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.mapper.GroupInfoMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GroupCommandPolicyTest {

    @Test
    void shouldUseSafeDefaultsForGroupWithoutConfiguration() {
        GroupInfoMapper mapper = mock(GroupInfoMapper.class);
        GroupCommandPolicy policy = policy(mapper, new CommandReleaseProperties(), Map.of());

        assertTrue(policy.evaluate("group-1", REGEX.ServerCheck).allowed());
        assertFalse(policy.evaluate("group-1", REGEX.DpsCompute).allowed());
        assertTrue(policy.evaluate("group-1", REGEX.Help).allowed());
    }

    @Test
    void shouldDenyProductionQueryWhenGroupCommandsAreDisabled() {
        GroupInfoMapper mapper = mock(GroupInfoMapper.class);
        GroupInfo groupInfo = new GroupInfo();
        groupInfo.setCommandsEnabled(false);
        when(mapper.findByOpenGroupId("group-1")).thenReturn(groupInfo);
        GroupCommandPolicy policy = policy(mapper, new CommandReleaseProperties(), Map.of());

        assertFalse(policy.evaluate("group-1", REGEX.ServerCheck).allowed());
        assertTrue(policy.evaluate("group-1", REGEX.GroupSettings).allowed());
    }

    @Test
    void shouldDenyOnlyConfiguredCommandAndKeepSystemCommandsAvailable() {
        GroupInfoMapper mapper = mock(GroupInfoMapper.class);
        GroupCommandPolicy policy = policy(mapper, new CommandReleaseProperties(),
                Map.of(REGEX.ServerCheck, false, REGEX.Help, false));

        GroupCommandPolicy.Decision denied = policy.evaluate("group-1", REGEX.ServerCheck);
        assertFalse(denied.allowed());
        assertTrue(denied.message().contains("开服状态"));
        assertTrue(policy.evaluate("group-1", REGEX.Help).allowed());
        assertTrue(policy.evaluate("group-1", REGEX.NewsAllNews).allowed());
    }

    @Test
    void shouldAllowExperimentalCommandOnlyWhenBothSwitchesAreEnabled() {
        GroupInfoMapper mapper = mock(GroupInfoMapper.class);
        GroupInfo groupInfo = new GroupInfo();
        groupInfo.setCommandsEnabled(true);
        groupInfo.setExperimentalEnabled(true);
        when(mapper.findByOpenGroupId("group-1")).thenReturn(groupInfo);
        CommandReleaseProperties releaseProperties = new CommandReleaseProperties();
        releaseProperties.setRuntimeMode(CommandReleaseProperties.RuntimeMode.TEST);
        GroupCommandPolicy policy = policy(mapper, releaseProperties, Map.of());

        assertTrue(policy.evaluate("group-1", REGEX.DpsCompute).allowed());
    }

    @Test
    void shouldHideDisabledAndExperimentalCommandsFromProductionMenu() {
        GroupInfoMapper mapper = mock(GroupInfoMapper.class);
        GroupCommandPolicy policy = policy(mapper, new CommandReleaseProperties(),
                Map.of(REGEX.ServerCheck, false));

        java.util.List<REGEX> definitions = policy.availableDefinitions("group-1");

        assertTrue(definitions.contains(REGEX.Help));
        assertFalse(definitions.contains(REGEX.ServerCheck));
        assertFalse(definitions.contains(REGEX.DpsCompute));
    }

    private static GroupCommandPolicy policy(GroupInfoMapper mapper,
                                             CommandReleaseProperties releaseProperties,
                                             Map<REGEX, Boolean> overrides) {
        GroupCommandSettingService settings = mock(GroupCommandSettingService.class);
        when(settings.findOverrides("group-1")).thenReturn(overrides);
        return new GroupCommandPolicy(mapper, releaseProperties, settings);
    }
}
