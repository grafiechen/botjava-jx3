package com.grafie.botjava.service;

import com.grafie.botjava.entity.CommandInvocation;
import com.grafie.botjava.entity.CommandInvocationStatus;
import com.grafie.botjava.entity.dto.common.AuthorDto;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.mapper.CommandInvocationMapper;
import com.grafie.botjava.observability.BotMetrics;
import com.grafie.botjava.util.SensitiveDataUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 以 best-effort 方式保存指令执行结果，审计库异常不得阻断消息链路。
 */
@Slf4j
@Service
public class CommandInvocationRecorder {

    private static final int MAX_FAILURE_LENGTH = 500;
    private final CommandInvocationMapper invocationMapper;
    private final BotMetrics botMetrics;

    public CommandInvocationRecorder(CommandInvocationMapper invocationMapper, BotMetrics botMetrics) {
        this.invocationMapper = invocationMapper;
        this.botMetrics = botMetrics;
    }

    public String newInvocationId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    public void record(String invocationId, GroupAtMessageCreateDto message, REGEX definition,
                       CommandInvocationStatus status, BotResponse.ResponseType responseType,
                       long elapsedMillis, Throwable failure) {
        CommandInvocation invocation = new CommandInvocation();
        invocation.setInvocationId(invocationId);
        invocation.setGroupOpenId(message.getGroupOpenid());
        invocation.setMemberOpenId(resolveMemberOpenId(message));
        invocation.setCommandName(definition.name());
        invocation.setCommandGroup(definition.getCommandGroup().name());
        invocation.setExternalCall(definition.usesExternalCall());
        invocation.setStatus(status);
        invocation.setResponseType(responseType == null ? null : responseType.name());
        invocation.setElapsedMillis(Math.max(0, elapsedMillis));
        invocation.setFailureSummary(summarize(failure));
        botMetrics.recordCommand(definition, status, responseType, elapsedMillis);
        try {
            invocationMapper.save(invocation);
        } catch (RuntimeException persistenceFailure) {
            log.error("保存指令调用记录失败，invocationId=>{}，command=>{}，reason=>{}",
                    invocationId, definition.name(), SensitiveDataUtil.summarize(persistenceFailure));
        }
    }

    private String resolveMemberOpenId(GroupAtMessageCreateDto message) {
        AuthorDto author = message.getAuthor();
        if (author == null) {
            return null;
        }
        return firstNotBlank(author.getMemberOpenid(), author.getId());
    }

    private String summarize(Throwable failure) {
        if (failure == null) {
            return null;
        }
        String summary = SensitiveDataUtil.summarize(failure);
        return summary.length() <= MAX_FAILURE_LENGTH
                ? summary
                : summary.substring(0, MAX_FAILURE_LENGTH);
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
