package com.grafie.botjava.service;

import com.grafie.botjava.config.CommandAuditProperties;
import com.grafie.botjava.entity.CommandInvocationStatus;
import com.grafie.botjava.mapper.CommandInvocationMapper;
import com.grafie.botjava.mapper.CommandInvocationSummary;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommandInvocationQueryServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-13T06:00:00Z"), ZoneId.of("Asia/Tokyo"));

    @Test
    void shouldBuildDisplayStatisticsAndUseDefaultDays() {
        CommandInvocationMapper mapper = mock(CommandInvocationMapper.class);
        CommandInvocationSummary summary = summary("ServerCheck", 12, 9, 123.6);
        when(mapper.summarizeByGroup(
                org.mockito.ArgumentMatchers.eq("group-1"),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class),
                org.mockito.ArgumentMatchers.eq(CommandInvocationStatus.SUCCESS)))
                .thenReturn(List.of(summary));
        CommandAuditProperties properties = new CommandAuditProperties();
        CommandInvocationQueryService service = new CommandInvocationQueryService(mapper, properties, CLOCK);

        CommandInvocationQueryService.Statistics result = service.statistics("group-1", null);

        assertEquals(7, result.days());
        assertEquals("开服状态", result.rows().get(0).displayName());
        assertEquals(75, result.rows().get(0).successRate());
        assertEquals(124, result.rows().get(0).averageElapsedMillis());
        ArgumentCaptor<LocalDateTime> since = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(mapper).summarizeByGroup(
                org.mockito.ArgumentMatchers.eq("group-1"), since.capture(),
                org.mockito.ArgumentMatchers.eq(CommandInvocationStatus.SUCCESS));
        assertEquals(LocalDateTime.of(2026, 7, 6, 15, 0), since.getValue());
    }

    @Test
    void shouldRejectOutOfRangeDays() {
        CommandAuditProperties properties = new CommandAuditProperties();
        properties.setStatisticsMaxDays(30);
        CommandInvocationQueryService service = new CommandInvocationQueryService(
                mock(CommandInvocationMapper.class), properties, CLOCK);

        assertThrows(IllegalArgumentException.class, () -> service.statistics("group-1", 31));
        assertThrows(IllegalArgumentException.class, () -> service.statistics(" ", 7));
    }

    private static CommandInvocationSummary summary(String commandName, long count,
                                                    long success, double average) {
        CommandInvocationSummary summary = mock(CommandInvocationSummary.class);
        when(summary.getCommandName()).thenReturn(commandName);
        when(summary.getInvocationCount()).thenReturn(count);
        when(summary.getSuccessCount()).thenReturn(success);
        when(summary.getAverageElapsedMillis()).thenReturn(average);
        return summary;
    }
}
