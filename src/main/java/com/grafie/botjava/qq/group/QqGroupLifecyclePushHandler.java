package com.grafie.botjava.qq.group;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.event.QqGroupLifecycleEventDto;
import com.grafie.botjava.service.push.GroupPushDispatcher;
import com.grafie.botjava.service.push.PushTaskDefinition;
import com.grafie.botjava.service.push.PushTaskRegistry;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 将 QQ WebSocket 群状态事件按当前群订阅状态转为主动消息。
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class QqGroupLifecyclePushHandler implements QqGroupLifecycleHandler {

    private final PushTaskRegistry taskRegistry;
    private final GroupPushDispatcher pushDispatcher;

    public QqGroupLifecyclePushHandler(PushTaskRegistry taskRegistry,
                                       GroupPushDispatcher pushDispatcher) {
        this.taskRegistry = taskRegistry;
        this.pushDispatcher = pushDispatcher;
    }

    @Override
    public boolean supports(QqGroupLifecycleEventType eventType) {
        return taskRegistry.findByQqWsEvent(eventType).isPresent();
    }

    @Override
    public void handle(QqGroupLifecycleEventType eventType, QqGroupLifecycleEventDto event) {
        PushTaskDefinition task = taskRegistry.findByQqWsEvent(eventType).orElseThrow();
        pushDispatcher.publishWsEventToGroup(
                task,
                event.getGroupOpenId(),
                fingerprint(task, event),
                BotResponse.text(buildText(task, event)));
    }

    private String buildText(PushTaskDefinition task, QqGroupLifecycleEventDto event) {
        StringBuilder text = new StringBuilder("【QQ WebSocket推送】")
                .append(task.displayName());
        if (event.getTimestamp() != null && !event.getTimestamp().isBlank()) {
            text.append("\n时间：").append(event.getTimestamp());
        }
        return text.toString();
    }

    private String fingerprint(PushTaskDefinition task, QqGroupLifecycleEventDto event) {
        String source = String.join("|",
                task.code(),
                clean(event.getGroupOpenId()),
                clean(event.getTimestamp()),
                clean(event.getOperatorMemberOpenId()));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("运行环境不支持 SHA-256", e);
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}