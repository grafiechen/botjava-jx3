package com.grafie.botjava.service.push;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.service.GroupMessageSender;
import com.grafie.botjava.service.GroupPushSubscriptionService;
import com.grafie.botjava.service.PushEventDeduplicationService;
import com.grafie.botjava.util.SensitiveDataUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Function;

/**
 * WS 实时事件与自定义定时任务共用的主动推送出口。
 */
@Slf4j
@Service
public class GroupPushDispatcher {

    private final GroupPushSubscriptionService subscriptionService;
    private final GroupMessageSender messageSender;
    private final PushEventDeduplicationService deduplicationService;
    private final TaskExecutor executor;

    public GroupPushDispatcher(GroupPushSubscriptionService subscriptionService,
                               GroupMessageSender messageSender,
                               PushEventDeduplicationService deduplicationService,
                               @Qualifier("groupPushExecutor") TaskExecutor executor) {
        this.subscriptionService = subscriptionService;
        this.messageSender = messageSender;
        this.deduplicationService = deduplicationService;
        this.executor = executor;
    }

    /**
     * 异步投递同一份推送内容，调用线程不会被 QQ HTTP 请求阻塞。
     */
    public void publish(PushTaskDefinition task, BotResponse response) {
        publish(task, ignored -> response);
    }

    /**
     * WS 事件专用入口。持久化唯一占位成功后才进入异步发送队列。
     */
    public void publishWsEvent(PushTaskDefinition task, String eventFingerprint,
                               BotResponse response) {
        if (!deduplicationService.tryClaim(task, eventFingerprint)) {
            return;
        }
        publish(task, response);
    }

    /**
     * QQ WS 群状态事件只允许回推事件所属群，并在真正发送前再次读取该群订阅状态。
     */
    public void publishWsEventToGroup(PushTaskDefinition task, String groupOpenId,
                                      String eventFingerprint, BotResponse response) {
        if (task == null || groupOpenId == null || groupOpenId.isBlank() || response == null) {
            throw new IllegalArgumentException("QQ WS 推送任务、目标群和内容不能为空");
        }
        try {
            executor.execute(() -> dispatchWsEventToGroup(
                    task, groupOpenId.trim(), eventFingerprint, response));
        } catch (RejectedExecutionException e) {
            log.error("群 WS 推送任务进入队列失败，taskCode=>{}，reason=>{}",
                    task.code(), SensitiveDataUtil.summarize(e), e);
        }
    }

    private void dispatchWsEventToGroup(PushTaskDefinition task, String groupOpenId,
                                        String eventFingerprint, BotResponse response) {
        if (!subscriptionService.isEnabled(groupOpenId, task)) {
            log.debug("群未开启 WS 推送，跳过事件，taskCode=>{}", task.code());
            return;
        }
        if (!deduplicationService.tryClaim(task, eventFingerprint)) {
            return;
        }
        try {
            GroupMessageSender.ActiveMessageResult result = messageSender.sendActive(groupOpenId, response);
            if (!result.sent()) {
                log.warn("群 WS 推送被主动消息策略拦截，taskCode=>{}，reason=>{}，retryAfterSeconds=>{}",
                        task.code(), result.message(), result.retryAfterSeconds());
            }
        } catch (RuntimeException e) {
            log.error("群 WS 推送投递失败，taskCode=>{}，reason=>{}",
                    task.code(), SensitiveDataUtil.summarize(e), e);
        }
    }

    /**
     * 异步投递按群动态生成的内容，供 Mongo 日常进度等定时任务使用。
     */
    public void publish(PushTaskDefinition task, Function<String, BotResponse> responseFactory) {
        if (task == null || responseFactory == null) {
            throw new IllegalArgumentException("推送任务和内容生成器不能为空");
        }
        try {
            executor.execute(() -> dispatch(task, responseFactory));
        } catch (RejectedExecutionException e) {
            log.error("群推送任务进入队列失败，taskCode=>{}，taskName=>{}，reason=>{}",
                    task.code(), task.displayName(), SensitiveDataUtil.summarize(e), e);
        }
    }

    private void dispatch(PushTaskDefinition task, Function<String, BotResponse> responseFactory) {
        List<String> groupOpenIds = subscriptionService.findEnabledGroupOpenIds(task);
        log.info("开始执行群推送任务，taskCode=>{}，taskName=>{}，source=>{}，targetGroups=>{}",
                task.code(), task.displayName(), task.source(), groupOpenIds.size());
        int sent = 0;
        int rejected = 0;
        int failed = 0;
        for (String groupOpenId : groupOpenIds) {
            try {
                BotResponse response = responseFactory.apply(groupOpenId);
                if (response == null) {
                    log.info("群推送任务未生成内容，taskCode=>{}", task.code());
                    continue;
                }
                GroupMessageSender.ActiveMessageResult result = messageSender.sendActive(groupOpenId, response);
                if (result.sent()) {
                    sent++;
                } else {
                    rejected++;
                    log.warn("群推送被主动消息策略拦截，taskCode=>{}，reason=>{}，retryAfterSeconds=>{}",
                            task.code(), result.message(), result.retryAfterSeconds());
                }
            } catch (RuntimeException e) {
                failed++;
                log.error("群推送投递失败，taskCode=>{}，reason=>{}",
                        task.code(), SensitiveDataUtil.summarize(e), e);
            }
        }
        log.info("结束执行群推送任务，taskCode=>{}，targetGroups=>{}，sent=>{}，rejected=>{}，failed=>{}",
                task.code(), groupOpenIds.size(), sent, rejected, failed);
    }
}
