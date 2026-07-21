package com.grafie.botjava.service;

import com.grafie.botjava.entity.CommandInvocationStatus;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.http.command.CommandArguments;
import com.grafie.botjava.jx3.http.command.Jx3CommandRegistry;
import com.grafie.botjava.jx3.http.command.ResolvedJx3Command;
import com.grafie.botjava.observability.RequestTraceContext;
import com.grafie.botjava.util.SensitiveDataUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 群指令统一执行模板，集中编排策略、参数、冷却、业务 Action、发送和审计。
 */
@Slf4j
@Service
public class GroupCommandExecutionService {

    private final GroupMessageSender groupMessageSender;
    private final Jx3CommandRegistry commandRegistry;
    private final GroupCommandPolicy commandPolicy;
    private final GroupCommandCooldownService cooldownService;
    private final UserCommandPreferenceService preferenceService;
    private final CommandInvocationRecorder invocationRecorder;

    public GroupCommandExecutionService(GroupMessageSender groupMessageSender,
                                        Jx3CommandRegistry commandRegistry,
                                        GroupCommandPolicy commandPolicy,
                                        GroupCommandCooldownService cooldownService,
                                        UserCommandPreferenceService preferenceService,
                                        CommandInvocationRecorder invocationRecorder) {
        this.groupMessageSender = groupMessageSender;
        this.commandRegistry = commandRegistry;
        this.commandPolicy = commandPolicy;
        this.cooldownService = cooldownService;
        this.preferenceService = preferenceService;
        this.invocationRecorder = invocationRecorder;
    }

    public void execute(GroupAtMessageCreateDto message) throws Exception {
        if (message.getContent() == null) {
            log.info("群消息内容为空，不执行指令");
            return;
        }
        ResolvedJx3Command command = commandRegistry.resolve(message.getContent()).orElse(null);
        if (command == null) {
            log.info("未匹配到可处理的群消息命令");
            return;
        }

        String invocationId = invocationRecorder.newInvocationId();
        long startNanos = System.nanoTime();
        try (RequestTraceContext.Scope ignored = RequestTraceContext.open(invocationId)) {
            executeResolved(invocationId, startNanos, message, command);
        }
    }

    private void executeResolved(String invocationId, long startNanos,
                                 GroupAtMessageCreateDto message,
                                 ResolvedJx3Command command) throws Exception {
        try {
            String memberRole = message.getAuthor() == null ? null : message.getAuthor().getMemberRole();
            if (!command.definition().getCommandAccess().allows(memberRole)) {
                sendAndRecord(invocationId, startNanos, message, command,
                        BotResponse.text("该指令仅限群主或管理员使用。"),
                        CommandInvocationStatus.PERMISSION_DENIED);
                return;
            }

            GroupCommandPolicy.Decision policyDecision = commandPolicy.evaluate(
                    message.getGroupOpenid(), command.definition());
            if (!policyDecision.allowed()) {
                sendAndRecord(invocationId, startNanos, message, command,
                        BotResponse.text(policyDecision.message()),
                        CommandInvocationStatus.FEATURE_DISABLED);
                return;
            }

            CommandArguments effectiveArguments = preferenceService.applyDefaults(message, command.arguments());
            if (command.definition().requiresRoleName() && effectiveArguments.roleName() == null) {
                sendAndRecord(invocationId, startNanos, message, command,
                        BotResponse.text("请提供角色名，或先使用：绑定角色 服务器 角色名"),
                        CommandInvocationStatus.INVALID_ARGUMENTS);
                return;
            }

            GroupCommandCooldownService.Decision cooldownDecision = cooldownService.tryAcquire(
                    message.getGroupOpenid(), command.definition());
            if (!cooldownDecision.allowed()) {
                log.info("群指令处于冷却期，不响应。invocationId=>{}，command=>{}，retryAfterSeconds=>{}",
                        invocationId, command.definition().name(),
                        cooldownDecision.retryAfterSeconds());
                recordInvocation(invocationId, startNanos, message, command,
                        CommandInvocationStatus.COOLDOWN, null, null);
                return;
            }

            BotResponse response;
            DeliveryOutcome deliveryOutcome = null;
            try {
                response = command.action().doRequest(
                        message, command.command(), command.definition(), effectiveArguments);
                if (response != null) {
                    deliveryOutcome = deliver(message, response);
                }
            } finally {
                cooldownService.complete(cooldownDecision);
            }

            if (response == null) {
                log.info("群消息命令未生成返回内容，invocationId=>{}，command=>{}",
                        invocationId, command.definition().name());
                recordInvocation(invocationId, startNanos, message, command,
                        CommandInvocationStatus.NO_RESPONSE, null, null);
                return;
            }
            recordInvocation(invocationId, startNanos, message, command,
                    deliveryOutcome.status(), deliveryOutcome.responseType(), null);
        } catch (Exception e) {
            log.error("群指令执行失败，invocationId=>{}，command=>{}，reason=>{}",
                    invocationId, command.definition().name(), SensitiveDataUtil.summarize(e));
            recordInvocation(invocationId, startNanos, message, command,
                    CommandInvocationStatus.FAILED, null, e);
            throw e;
        }
    }

    private DeliveryOutcome deliver(GroupAtMessageCreateDto message, BotResponse response) {
        if (response.getDeliveryMode() == null) {
            throw new IllegalArgumentException("业务返回发送方式不能为空");
        }
        if (response.getDeliveryMode() == BotResponse.DeliveryMode.REPLY) {
            groupMessageSender.send(message, response);
            return DeliveryOutcome.success(response.getResponseType());
        }

        GroupMessageSender.ActiveMessageResult result = groupMessageSender.sendActive(
                message.getGroupOpenid(), response);
        if (result.sent()) {
            return DeliveryOutcome.success(response.getResponseType());
        }

        groupMessageSender.send(message, BotResponse.text(activeMessageFeedback(result)));
        CommandInvocationStatus status = result.retryAfterSeconds() > 0
                ? CommandInvocationStatus.COOLDOWN : CommandInvocationStatus.FEATURE_DISABLED;
        return new DeliveryOutcome(status, BotResponse.ResponseType.TEXT);
    }

    private String activeMessageFeedback(GroupMessageSender.ActiveMessageResult result) {
        String message = result.message() == null || result.message().isBlank()
                ? "群主动消息暂时无法发送。" : result.message().trim();
        if (result.retryAfterSeconds() <= 0) {
            return message;
        }
        boolean hasPunctuation = message.endsWith("。") || message.endsWith("！")
                || message.endsWith("？") || message.endsWith("；");
        return message + (hasPunctuation ? "" : "，")
                + "请在 " + result.retryAfterSeconds() + " 秒后重试。";
    }

    private void sendAndRecord(String invocationId, long startNanos,
                               GroupAtMessageCreateDto message, ResolvedJx3Command command,
                               BotResponse response, CommandInvocationStatus status) {
        groupMessageSender.send(message, response);
        recordInvocation(invocationId, startNanos, message, command,
                status, response.getResponseType(), null);
    }

    private void recordInvocation(String invocationId, long startNanos,
                                  GroupAtMessageCreateDto message, ResolvedJx3Command command,
                                  CommandInvocationStatus status,
                                  BotResponse.ResponseType responseType, Throwable failure) {
        long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;
        invocationRecorder.record(invocationId, message, command.definition(), status,
                responseType, elapsedMillis, failure);
    }

    private record DeliveryOutcome(CommandInvocationStatus status,
                                   BotResponse.ResponseType responseType) {
        private static DeliveryOutcome success(BotResponse.ResponseType responseType) {
            return new DeliveryOutcome(CommandInvocationStatus.SUCCESS, responseType);
        }
    }
}
