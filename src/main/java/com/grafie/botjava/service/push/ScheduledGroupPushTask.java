package com.grafie.botjava.service.push;

import com.grafie.botjava.entity.dto.common.BotResponse;

/**
 * 自定义定时推送扩展点。实现类负责按群拼装数据，订阅和发送由 GroupPushDispatcher 统一完成。
 */
public interface ScheduledGroupPushTask {

    PushTaskDefinition definition();

    BotResponse buildResponse(String groupOpenId);
}
