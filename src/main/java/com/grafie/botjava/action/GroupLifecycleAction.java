package com.grafie.botjava.action;

import com.grafie.botjava.config.BotMessageAction;
import com.grafie.botjava.entity.dto.group.event.QqGroupLifecycleEventDto;
import com.grafie.botjava.entity.dto.payload.Payload;
import com.grafie.botjava.qq.group.QqGroupLifecycleEventType;
import com.grafie.botjava.qq.group.QqGroupLifecycleHandlerRegistry;
import com.grafie.botjava.util.ObjectMapperUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * QQ 机器人群生命周期与主动消息授权事件入口。
 */
@Slf4j
@BotMessageAction
public class GroupLifecycleAction extends BaseAction {

    private final QqGroupLifecycleHandlerRegistry handlerRegistry;

    public GroupLifecycleAction(QqGroupLifecycleHandlerRegistry handlerRegistry) {
        this.handlerRegistry = handlerRegistry;
    }

    @Override
    protected Object deal(Payload payload) throws Exception {
        QqGroupLifecycleEventType eventType = QqGroupLifecycleEventType.fromPayloadType(payload.getT())
                .orElseThrow(() -> new IllegalArgumentException("不支持的 QQ 群状态事件：" + payload.getT()));
        QqGroupLifecycleEventDto event = ObjectMapperUtil.readValue(
                payload.getD(), QqGroupLifecycleEventDto.class);
        if (event.getGroupOpenId() == null || event.getGroupOpenId().isBlank()) {
            throw new IllegalArgumentException("QQ 群状态事件缺少 group_openid");
        }
        int handled = handlerRegistry.dispatch(eventType, event);
        log.info("QQ 群状态事件处理完成，eventType=>{}，handlerCount=>{}",
                eventType, handled);
        return null;
    }
}
