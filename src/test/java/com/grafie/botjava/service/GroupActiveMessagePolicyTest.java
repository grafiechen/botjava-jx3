package com.grafie.botjava.service;

import com.grafie.botjava.config.ActiveMessageProperties;
import com.grafie.botjava.entity.GroupInfo;
import com.grafie.botjava.mapper.GroupInfoMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroupActiveMessagePolicyTest {

    @Test
    void shouldRequireExplicitGroupAuthorization() {
        GroupInfoMapper mapper = mock(GroupInfoMapper.class);
        GroupActiveMessagePolicy policy = policy(mapper, 60);

        assertFalse(policy.acquire("group-1").allowed());

        GroupInfo disabled = new GroupInfo();
        disabled.setActiveMessagesEnabled(false);
        when(mapper.findByOpenGroupId("group-1")).thenReturn(disabled);
        assertFalse(policy.acquire("group-1").allowed());

        GroupInfo missingPlatformAuthorization = new GroupInfo();
        missingPlatformAuthorization.setActiveMessagesEnabled(true);
        missingPlatformAuthorization.setActiveMessagesPlatformAllowed(false);
        when(mapper.findByOpenGroupId("group-1")).thenReturn(missingPlatformAuthorization);
        assertFalse(policy.acquire("group-1").allowed());
    }

    @Test
    void shouldApplyIndependentGroupWindowAndAllowRollback() {
        GroupInfoMapper mapper = mock(GroupInfoMapper.class);
        GroupInfo enabled = new GroupInfo();
        enabled.setActiveMessagesEnabled(true);
        enabled.setActiveMessagesPlatformAllowed(true);
        enabled.setActiveMessageReservedAt(Instant.parse("2026-07-13T06:00:00Z"));
        when(mapper.findByOpenGroupId("group-1")).thenReturn(enabled);
        when(mapper.reserveActiveMessage(
                eq("group-1"),
                eq(Instant.parse("2026-07-13T06:00:00Z")),
                eq(Instant.parse("2026-07-13T05:59:00Z")),
                anyString()
        )).thenReturn(1, 0, 1);
        GroupActiveMessagePolicy policy = policy(mapper, 60);

        GroupActiveMessagePolicy.Permit first = policy.acquire("group-1");
        GroupActiveMessagePolicy.Permit denied = policy.acquire("group-1");
        assertTrue(first.allowed());
        assertFalse(denied.allowed());
        assertEquals(60, denied.retryAfterSeconds());

        policy.rollback(first);
        verify(mapper).rollbackActiveMessage("group-1", first.reservationId());
        assertTrue(policy.acquire("group-1").allowed());
    }

    private static GroupActiveMessagePolicy policy(GroupInfoMapper mapper, int intervalSeconds) {
        ActiveMessageProperties properties = new ActiveMessageProperties();
        properties.setActiveMessageMinIntervalSeconds(intervalSeconds);
        return new GroupActiveMessagePolicy(mapper, properties,
                Clock.fixed(Instant.parse("2026-07-13T06:00:00Z"), ZoneOffset.UTC));
    }
}
