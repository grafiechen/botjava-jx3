package com.grafie.botjava.service;

import com.grafie.botjava.entity.CommandInvocation;
import com.grafie.botjava.entity.CommandInvocationStatus;
import com.grafie.botjava.entity.dto.common.AuthorDto;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.mapper.CommandInvocationMapper;
import com.grafie.botjava.observability.BotMetrics;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CommandInvocationRecorderTest {

    @Test
    void shouldPersistMetadataWithoutRawCommandContent() {
        CommandInvocationMapper mapper = mock(CommandInvocationMapper.class);
        BotMetrics metrics = mock(BotMetrics.class);
        CommandInvocationRecorder recorder = new CommandInvocationRecorder(mapper, metrics);

        recorder.record("trace-123", message(), REGEX.ServerCheck, CommandInvocationStatus.SUCCESS,
                BotResponse.ResponseType.TEXT, 42, null);

        ArgumentCaptor<CommandInvocation> captor = ArgumentCaptor.forClass(CommandInvocation.class);
        verify(mapper).save(captor.capture());
        CommandInvocation saved = captor.getValue();
        assertEquals("trace-123", saved.getInvocationId());
        assertEquals("group-1", saved.getGroupOpenId());
        assertEquals("member-1", saved.getMemberOpenId());
        assertEquals("ServerCheck", saved.getCommandName());
        assertEquals("FREE", saved.getCommandGroup());
        assertTrue(saved.isExternalCall());
        assertEquals(CommandInvocationStatus.SUCCESS, saved.getStatus());
        assertEquals("TEXT", saved.getResponseType());
        assertEquals(42, saved.getElapsedMillis());
        assertNull(saved.getFailureSummary());
        assertFalse(saved.toString().contains("开服 乾坤一掷"));
        verify(metrics).recordCommand(REGEX.ServerCheck, CommandInvocationStatus.SUCCESS,
                BotResponse.ResponseType.TEXT, 42);
    }

    @Test
    void shouldRedactSensitiveValuesInFailureSummary() {
        CommandInvocationMapper mapper = mock(CommandInvocationMapper.class);
        CommandInvocationRecorder recorder = new CommandInvocationRecorder(mapper, mock(BotMetrics.class));

        recorder.record("trace-456", message(), REGEX.ServerCheck, CommandInvocationStatus.FAILED,
                null, 7, new IllegalStateException("upstream token=real-token ticket:real-ticket failed"));

        ArgumentCaptor<CommandInvocation> captor = ArgumentCaptor.forClass(CommandInvocation.class);
        verify(mapper).save(captor.capture());
        String summary = captor.getValue().getFailureSummary();
        assertTrue(summary.startsWith("IllegalStateException:"));
        assertTrue(summary.contains("token=******"));
        assertTrue(summary.contains("ticket=******"));
        assertFalse(summary.contains("real-token"));
        assertFalse(summary.contains("real-ticket"));
    }

    @Test
    void shouldNotBreakMessageFlowWhenAuditPersistenceFails() {
        CommandInvocationMapper mapper = mock(CommandInvocationMapper.class);
        doThrow(new IllegalStateException("database unavailable")).when(mapper).save(any());
        CommandInvocationRecorder recorder = new CommandInvocationRecorder(mapper, mock(BotMetrics.class));

        assertDoesNotThrow(() -> recorder.record("trace-789", message(), REGEX.Help,
                CommandInvocationStatus.SUCCESS, BotResponse.ResponseType.TEXT, 1, null));
    }

    private static GroupAtMessageCreateDto message() {
        AuthorDto author = new AuthorDto();
        author.setMemberOpenid("member-1");
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        message.setGroupOpenid("group-1");
        message.setContent("开服 乾坤一掷");
        message.setAuthor(author);
        return message;
    }
}
