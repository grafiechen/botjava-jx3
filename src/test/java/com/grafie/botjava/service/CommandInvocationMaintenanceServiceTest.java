package com.grafie.botjava.service;

import com.grafie.botjava.config.CommandAuditProperties;
import com.grafie.botjava.mapper.CommandInvocationMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommandInvocationMaintenanceServiceTest {

    @Test
    void shouldDeleteRecordsOlderThanRetentionWindow() {
        CommandInvocationMapper mapper = mock(CommandInvocationMapper.class);
        CommandAuditProperties properties = new CommandAuditProperties();
        properties.setRetentionDays(30);
        Clock clock = Clock.fixed(Instant.parse("2026-07-13T06:00:00Z"), ZoneId.of("Asia/Tokyo"));
        when(mapper.deleteByCreateTimeBefore(org.mockito.ArgumentMatchers.any())).thenReturn(42L);

        long deleted = new CommandInvocationMaintenanceService(mapper, properties, clock).cleanupExpired();

        assertEquals(42, deleted);
        ArgumentCaptor<LocalDateTime> threshold = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(mapper).deleteByCreateTimeBefore(threshold.capture());
        assertEquals(LocalDateTime.of(2026, 6, 13, 15, 0), threshold.getValue());
    }
}
