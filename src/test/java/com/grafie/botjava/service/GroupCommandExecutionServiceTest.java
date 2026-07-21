package com.grafie.botjava.service;

import com.grafie.botjava.entity.CommandInvocationStatus;
import com.grafie.botjava.entity.dto.common.AuthorDto;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.command.CommandArguments;
import com.grafie.botjava.jx3.http.command.Jx3CommandRegistry;
import com.grafie.botjava.jx3.http.command.ResolvedJx3Command;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.observability.RequestTraceContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroupCommandExecutionServiceTest {

    @Test
    void shouldExecuteResolvedCommandWithEffectiveArgumentsAndRecordSuccess() throws Exception {
        Fixture fixture = new Fixture();
        CommandArguments raw = CommandArguments.of(Map.of("server", "乾坤一掷"));
        CommandArguments effective = raw.withDefaults("乾坤一掷", null);
        ResolvedJx3Command command = fixture.command(REGEX.ServerCheck, raw);
        when(fixture.registry.resolve("/开服 乾坤一掷")).thenReturn(Optional.of(command));
        when(fixture.preferences.applyDefaults(fixture.message, raw)).thenReturn(effective);
        BotResponse response = BotResponse.text("服务器[乾坤一掷]，已开服");
        AtomicReference<String> traceInsideAction = new AtomicReference<>();
        when(fixture.action.doRequest(any(), any(), any(), any())).thenAnswer(invocation -> {
            traceInsideAction.set(RequestTraceContext.currentId().orElse(null));
            return response;
        });

        fixture.service.execute(fixture.message);

        verify(fixture.action).doRequest(fixture.message, "开服 乾坤一掷",
                REGEX.ServerCheck, effective);
        verify(fixture.sender).send(fixture.message, response);
        verify(fixture.cooldown).complete(fixture.permit);
        fixture.verifyRecorded(REGEX.ServerCheck, CommandInvocationStatus.SUCCESS,
                BotResponse.ResponseType.TEXT, null);
        assertEquals("invocation-1", traceInsideAction.get());
        assertFalse(RequestTraceContext.currentId().isPresent());
    }

    @Test
    void shouldRejectAdministrativeCommandBeforePolicyAndCooldown() throws Exception {
        Fixture fixture = new Fixture();
        fixture.resolve(REGEX.BindServerCalendar, "绑定 乾坤一掷", CommandArguments.of(Map.of()));

        fixture.service.execute(fixture.message);

        verify(fixture.policy, never()).evaluate(any(), any());
        verify(fixture.cooldown, never()).tryAcquire(any(), any());
        verify(fixture.action, never()).doRequest(any(), any(), any(), any());
        ArgumentCaptor<BotResponse> response = ArgumentCaptor.forClass(BotResponse.class);
        verify(fixture.sender).send(org.mockito.ArgumentMatchers.eq(fixture.message), response.capture());
        assertEquals("该指令仅限群主或管理员使用。", response.getValue().getContent());
        fixture.verifyRecorded(REGEX.BindServerCalendar,
                CommandInvocationStatus.PERMISSION_DENIED, BotResponse.ResponseType.TEXT, null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"owner", "admin"})
    void shouldAllowOwnerAndAdministratorToBindGroupServer(String memberRole) throws Exception {
        Fixture fixture = new Fixture();
        fixture.message.getAuthor().setMemberRole(memberRole);
        fixture.message.setContent("绑定 乾坤一掷");
        CommandArguments arguments = CommandArguments.of(Map.of("server", "乾坤一掷"));
        fixture.resolve(REGEX.BindServerCalendar, "绑定 乾坤一掷", arguments);
        BotResponse response = BotResponse.text("默认服务器设置成功，[乾坤一掷]");
        when(fixture.action.doRequest(any(), any(), any(), any())).thenReturn(response);

        fixture.service.execute(fixture.message);

        verify(fixture.policy).evaluate("group-1", REGEX.BindServerCalendar);
        verify(fixture.action).doRequest(fixture.message, "绑定 乾坤一掷",
                REGEX.BindServerCalendar, arguments);
        verify(fixture.sender).send(fixture.message, response);
        fixture.verifyRecorded(REGEX.BindServerCalendar,
                CommandInvocationStatus.SUCCESS, BotResponse.ResponseType.TEXT, null);
    }

    @Test
    void shouldPromptForMissingRoleWithoutConsumingCooldown() throws Exception {
        Fixture fixture = new Fixture();
        fixture.message.setContent("装备");
        fixture.resolve(REGEX.RoleAttribute, "装备", CommandArguments.of(Map.of()));

        fixture.service.execute(fixture.message);

        verify(fixture.cooldown, never()).tryAcquire(any(), any());
        verify(fixture.action, never()).doRequest(any(), any(), any(), any());
        ArgumentCaptor<BotResponse> response = ArgumentCaptor.forClass(BotResponse.class);
        verify(fixture.sender).send(org.mockito.ArgumentMatchers.eq(fixture.message), response.capture());
        assertTrue(response.getValue().getContent().contains("绑定角色 服务器 角色名"));
        fixture.verifyRecorded(REGEX.RoleAttribute,
                CommandInvocationStatus.INVALID_ARGUMENTS, BotResponse.ResponseType.TEXT, null);
    }

    @Test
    void shouldSilentlyRecordCooldownWithoutExecutingOrSending() throws Exception {
        Fixture fixture = new Fixture();
        fixture.resolve(REGEX.ServerCheck, "开服 乾坤一掷", CommandArguments.of(Map.of()));
        when(fixture.cooldown.tryAcquire("group-1", REGEX.ServerCheck))
                .thenReturn(GroupCommandCooldownService.Decision.deny(17));

        fixture.service.execute(fixture.message);

        verify(fixture.action, never()).doRequest(any(), any(), any(), any());
        verify(fixture.sender, never()).send(any(), any());
        fixture.verifyRecorded(REGEX.ServerCheck, CommandInvocationStatus.COOLDOWN, null, null);
    }

    @Test
    void shouldRecordNoResponseAndStillCompleteReservation() throws Exception {
        Fixture fixture = new Fixture();
        fixture.resolve(REGEX.ServerCheck, "开服 乾坤一掷", CommandArguments.of(Map.of()));
        when(fixture.action.doRequest(any(), any(), any(), any())).thenReturn(null);

        fixture.service.execute(fixture.message);

        verify(fixture.sender, never()).send(any(), any());
        verify(fixture.cooldown).complete(fixture.permit);
        fixture.verifyRecorded(REGEX.ServerCheck, CommandInvocationStatus.NO_RESPONSE, null, null);
    }

    @Test
    void shouldCompleteReservationAndRecordFailureWhenSendingFails() throws Exception {
        Fixture fixture = new Fixture();
        fixture.resolve(REGEX.ServerCheck, "开服 乾坤一掷", CommandArguments.of(Map.of()));
        when(fixture.action.doRequest(any(), any(), any(), any())).thenReturn(BotResponse.text("已开服"));
        IllegalStateException failure = new IllegalStateException("QQ send failed token=secret-value");
        doThrow(failure).when(fixture.sender).send(any(), any());

        assertThrows(IllegalStateException.class, () -> fixture.service.execute(fixture.message));

        verify(fixture.cooldown).complete(fixture.permit);
        fixture.verifyRecorded(REGEX.ServerCheck, CommandInvocationStatus.FAILED, null, failure);
        assertFalse(RequestTraceContext.currentId().isPresent());
    }

    @Test
    void shouldDispatchActiveResponseWithoutPassiveReplyFields() throws Exception {
        Fixture fixture = new Fixture();
        fixture.message.getAuthor().setMemberRole("admin");
        fixture.message.setContent("群公告 今晚八点开团");
        fixture.resolve(REGEX.GroupAnnouncement, "群公告 今晚八点开团",
                CommandArguments.of(Map.of("text", "今晚八点开团")));
        BotResponse response = BotResponse.text("今晚八点开团").asActiveMessage();
        when(fixture.action.doRequest(any(), any(), any(), any())).thenReturn(response);
        when(fixture.sender.sendActive("group-1", response))
                .thenReturn(GroupMessageSender.ActiveMessageResult.delivered());

        fixture.service.execute(fixture.message);

        verify(fixture.sender).sendActive("group-1", response);
        verify(fixture.sender, never()).send(fixture.message, response);
        fixture.verifyRecorded(REGEX.GroupAnnouncement,
                CommandInvocationStatus.SUCCESS, BotResponse.ResponseType.TEXT, null);
    }

    @Test
    void shouldReplyWhenActiveMessageIsNotAuthorized() throws Exception {
        Fixture fixture = new Fixture();
        fixture.message.getAuthor().setMemberRole("owner");
        fixture.message.setContent("群公告 今晚八点开团");
        fixture.resolve(REGEX.GroupAnnouncement, "群公告 今晚八点开团",
                CommandArguments.of(Map.of("text", "今晚八点开团")));
        BotResponse response = BotResponse.text("今晚八点开团").asActiveMessage();
        when(fixture.action.doRequest(any(), any(), any(), any())).thenReturn(response);
        when(fixture.sender.sendActive("group-1", response)).thenReturn(
                GroupMessageSender.ActiveMessageResult.rejected("本群未开启主动消息。", 0));

        fixture.service.execute(fixture.message);

        ArgumentCaptor<BotResponse> feedback = ArgumentCaptor.forClass(BotResponse.class);
        verify(fixture.sender).send(
                org.mockito.ArgumentMatchers.eq(fixture.message), feedback.capture());
        assertEquals("本群未开启主动消息。", feedback.getValue().getContent());
        fixture.verifyRecorded(REGEX.GroupAnnouncement,
                CommandInvocationStatus.FEATURE_DISABLED, BotResponse.ResponseType.TEXT, null);
    }

    @Test
    void shouldReplyWithRetryDelayWhenActiveMessageIsRateLimited() throws Exception {
        Fixture fixture = new Fixture();
        fixture.message.getAuthor().setMemberRole("admin");
        fixture.message.setContent("群公告 今晚八点开团");
        fixture.resolve(REGEX.GroupAnnouncement, "群公告 今晚八点开团",
                CommandArguments.of(Map.of("text", "今晚八点开团")));
        BotResponse response = BotResponse.text("今晚八点开团").asActiveMessage();
        when(fixture.action.doRequest(any(), any(), any(), any())).thenReturn(response);
        when(fixture.sender.sendActive("group-1", response)).thenReturn(
                GroupMessageSender.ActiveMessageResult.rejected("本群主动消息处于频控期。", 12));

        fixture.service.execute(fixture.message);

        ArgumentCaptor<BotResponse> feedback = ArgumentCaptor.forClass(BotResponse.class);
        verify(fixture.sender).send(
                org.mockito.ArgumentMatchers.eq(fixture.message), feedback.capture());
        assertEquals("本群主动消息处于频控期。请在 12 秒后重试。", feedback.getValue().getContent());
        fixture.verifyRecorded(REGEX.GroupAnnouncement,
                CommandInvocationStatus.COOLDOWN, BotResponse.ResponseType.TEXT, null);
    }

    private static class Fixture {
        private final GroupMessageSender sender = mock(GroupMessageSender.class);
        private final Jx3CommandRegistry registry = mock(Jx3CommandRegistry.class);
        private final GroupCommandPolicy policy = mock(GroupCommandPolicy.class);
        private final GroupCommandCooldownService cooldown = mock(GroupCommandCooldownService.class);
        private final UserCommandPreferenceService preferences = mock(UserCommandPreferenceService.class);
        private final CommandInvocationRecorder recorder = mock(CommandInvocationRecorder.class);
        private final Jx3BaseAction action = mock(Jx3BaseAction.class);
        private final GroupAtMessageCreateDto message = message();
        private final GroupCommandCooldownService.Decision permit =
                GroupCommandCooldownService.Decision.allow(
                        "group-1", REGEX.ServerCheck, "reservation-1", Duration.ofSeconds(30));
        private final GroupCommandExecutionService service;

        private Fixture() {
            when(policy.evaluate(any(), any())).thenReturn(GroupCommandPolicy.Decision.allow());
            when(cooldown.tryAcquire(any(), any())).thenReturn(permit);
            when(preferences.applyDefaults(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
            when(recorder.newInvocationId()).thenReturn("invocation-1");
            service = new GroupCommandExecutionService(
                    sender, registry, policy, cooldown, preferences, recorder);
        }

        private void resolve(REGEX definition, String commandText, CommandArguments arguments) {
            when(registry.resolve(message.getContent()))
                    .thenReturn(Optional.of(command(definition, arguments, commandText)));
        }

        private ResolvedJx3Command command(REGEX definition, CommandArguments arguments) {
            return command(definition, arguments, "开服 乾坤一掷");
        }

        private ResolvedJx3Command command(REGEX definition, CommandArguments arguments, String commandText) {
            return new ResolvedJx3Command(commandText, definition, arguments, action);
        }

        private void verifyRecorded(REGEX definition, CommandInvocationStatus status,
                                    BotResponse.ResponseType responseType, Throwable failure) {
            verify(recorder).record(org.mockito.ArgumentMatchers.eq("invocation-1"),
                    org.mockito.ArgumentMatchers.eq(message),
                    org.mockito.ArgumentMatchers.eq(definition),
                    org.mockito.ArgumentMatchers.eq(status),
                    responseType == null
                            ? org.mockito.ArgumentMatchers.isNull()
                            : org.mockito.ArgumentMatchers.eq(responseType),
                    anyLong(),
                    failure == null
                            ? org.mockito.ArgumentMatchers.isNull()
                            : org.mockito.ArgumentMatchers.same(failure));
        }

        private static GroupAtMessageCreateDto message() {
            GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
            message.setId("message-1");
            message.setContent("/开服 乾坤一掷");
            message.setGroupOpenid("group-1");
            AuthorDto author = new AuthorDto();
            author.setMemberOpenid("user-1");
            author.setMemberRole("member");
            message.setAuthor(author);
            return message;
        }
    }
}
