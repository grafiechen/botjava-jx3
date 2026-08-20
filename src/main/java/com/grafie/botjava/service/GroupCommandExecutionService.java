package com.grafie.botjava.service;

import com.grafie.botjava.entity.CommandInvocationStatus;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.http.command.CommandArguments;
import com.grafie.botjava.jx3.http.command.Jx3CommandRegistry;
import com.grafie.botjava.jx3.http.command.ResolvedJx3Command;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.observability.RequestTraceContext;
import com.grafie.botjava.util.SensitiveDataUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * 群指令统一执行模板，集中编排策略、参数、冷却、业务 Action、发送和审计。
 */
@Slf4j
@Service
public class GroupCommandExecutionService {

    private static final int MAX_LOGGED_MESSAGE_LENGTH = 1000;

    private final GroupMessageSender groupMessageSender;
    private final ObjectProvider<Jx3CommandRegistry> commandRegistryProvider;
    private final GroupCommandPolicy commandPolicy;
    private final GroupCommandCooldownService cooldownService;
    private final UserCommandPreferenceService preferenceService;
    private final CommandInvocationRecorder invocationRecorder;
    private final GroupCommandPermissionConfiguration permissionConfiguration;

    public GroupCommandExecutionService(GroupMessageSender groupMessageSender,
                                        ObjectProvider<Jx3CommandRegistry> commandRegistryProvider,
                                        GroupCommandPolicy commandPolicy,
                                        GroupCommandCooldownService cooldownService,
                                        UserCommandPreferenceService preferenceService,
                                        CommandInvocationRecorder invocationRecorder,
                                        GroupCommandPermissionConfiguration permissionConfiguration) {
        this.groupMessageSender = groupMessageSender;
        this.commandRegistryProvider = commandRegistryProvider;
        this.commandPolicy = commandPolicy;
        this.cooldownService = cooldownService;
        this.preferenceService = preferenceService;
        this.invocationRecorder = invocationRecorder;
        this.permissionConfiguration = permissionConfiguration;
    }

    public void execute(GroupAtMessageCreateDto message) throws Exception {
        String loggedContent = messageContentForLog(message.getContent());
        log.info("收到群消息，content=>{}", loggedContent);
        if (message.getContent() == null) {
            log.info("群消息内容为空，不执行指令");
            return;
        }
        Jx3CommandRegistry commandRegistry = commandRegistryProvider.getIfAvailable();
        if (commandRegistry == null) {
            String normalizedCommand = REGEX.normalizeCommand(message.getContent());
            REGEX definition = REGEX.matchEnum(normalizedCommand);
            if (definition != null) {
                groupMessageSender.send(message, BotResponse.text("该功能未开启。"));
                log.info("JX3API HTTP 指令未启用，已返回功能关闭提示。command=>{}", definition.name());
            } else {
                log.info("JX3API HTTP 指令未启用，且群消息未匹配 JX3 指令，content=>{}", loggedContent);
            }
            return;
        }
        ResolvedJx3Command command = commandRegistry.resolve(message.getContent()).orElse(null);
        if (command == null) {
            log.info("未匹配到可处理的群消息命令，content=>{}", loggedContent);
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
            String memberOpenId = message.getAuthor() == null ? null
                    : firstNotBlank(message.getAuthor().getMemberOpenid(), message.getAuthor().getId());
            if (!isAllowedByPermissionConfiguration(message.getGroupOpenid(), memberOpenId, command.definition())) {
                sendAndRecord(invocationId, startNanos, message, command,
                        BotResponse.text("该指令仅限授权成员使用。"),
                        CommandInvocationStatus.PERMISSION_DENIED);
                return;
            }
            if (!permissionConfiguration.isPermissionCommand(command.definition())
                    && !command.definition().getCommandAccess().allows(memberRole)) {
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
                        BotResponse.text("请提供角色名，或先使用：绑定角色 服务器 角色名 门派"),
                        CommandInvocationStatus.INVALID_ARGUMENTS);
                return;
            }

            GroupCommandCooldownService.Decision cooldownDecision = cooldownService.tryAcquire(
                    message.getGroupOpenid(), command.definition());
            if (!cooldownDecision.allowed()) {
                log.info("群指令处于冷却期，已返回剩余 CD。invocationId=>{}，command=>{}，retryAfterSeconds=>{}",
                        invocationId, command.definition().name(),
                        cooldownDecision.retryAfterSeconds());
                sendAndRecord(invocationId, startNanos, message, command,
                        BotResponse.text(cooldownFeedback(cooldownDecision.retryAfterSeconds())),
                        CommandInvocationStatus.COOLDOWN);
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
                    invocationId, command.definition().name(), SensitiveDataUtil.summarize(e), e);
            recordInvocation(invocationId, startNanos, message, command,
                    CommandInvocationStatus.FAILED, null, e);
            throw e;
        }
    }

    private String messageContentForLog(String content) {
        if (content == null) {
            return null;
        }
        StringBuilder singleLine = new StringBuilder(content.length());
        boolean previousWhitespace = false;
        for (int index = 0; index < content.length(); index++) {
            char current = content.charAt(index);
            boolean whitespace = Character.isWhitespace(current)
                    || Character.isISOControl(current)
                    || current == '\u2028' || current == '\u2029';
            if (whitespace) {
                if (!previousWhitespace) {
                    singleLine.append(' ');
                }
                previousWhitespace = true;
            } else {
                singleLine.append(current);
                previousWhitespace = false;
            }
        }
        String redacted = SensitiveDataUtil.redactText(singleLine.toString().trim());
        if (redacted.length() <= MAX_LOGGED_MESSAGE_LENGTH) {
            return redacted;
        }
        return redacted.substring(0, MAX_LOGGED_MESSAGE_LENGTH)
                + "...(已截断，原长度=" + redacted.length() + ")";
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String cooldownFeedback(long retryAfterSeconds) {
        long seconds = Math.max(1, retryAfterSeconds);
        return "指令冷却中，请在 " + seconds + " 秒后重试。";
    }


    private boolean isAllowedByPermissionConfiguration(String groupOpenId, String memberOpenId, REGEX command) {
        if (!permissionConfiguration.isPermissionCommand(command)) {
            return true;
        }
        return permissionConfiguration.evaluate(groupOpenId, memberOpenId, command)
                .map(GroupCommandPermissionConfiguration.Decision::allowed)
                .orElse(false);
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
