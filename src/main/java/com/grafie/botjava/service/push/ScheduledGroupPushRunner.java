package com.grafie.botjava.service.push;

import org.springframework.stereotype.Service;

/**
 * 自定义定时推送的模板入口。定时器只提交任务实现，不直接处理订阅和 QQ 发送。
 */
@Service
public class ScheduledGroupPushRunner {

    private final GroupPushDispatcher dispatcher;

    public ScheduledGroupPushRunner(GroupPushDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public void run(ScheduledGroupPushTask task) {
        if (task == null || task.definition() == null) {
            throw new IllegalArgumentException("定时推送任务及其定义不能为空");
        }
        if (task.definition().source() != PushTaskSource.SCHEDULED) {
            throw new IllegalArgumentException("ScheduledGroupPushRunner 只接受 SCHEDULED 任务");
        }
        dispatcher.publish(task.definition(), task::buildResponse);
    }
}