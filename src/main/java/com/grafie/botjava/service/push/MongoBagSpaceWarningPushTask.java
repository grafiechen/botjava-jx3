package com.grafie.botjava.service.push;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.service.ScriptStatusService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "bot.mongodb", name = "enabled", havingValue = "true")
public class MongoBagSpaceWarningPushTask implements ScheduledGroupPushTask {

    private final PushTaskDefinition definition;
    private final ScheduledGroupPushRunner runner;
    private final ScriptStatusService scriptStatusService;

    public MongoBagSpaceWarningPushTask(PushTaskRegistry registry,
                                        ScheduledGroupPushRunner runner,
                                        ScriptStatusService scriptStatusService) {
        this.definition = registry.find(PushTaskRegistry.MONGO_BAG_SPACE_WARNING).orElseThrow();
        this.runner = runner;
        this.scriptStatusService = scriptStatusService;
    }

    @Scheduled(cron = "${bot.push.mongo-bag-warning.cron:0 0 9 * * *}")
    public void publishAtNine() {
        runner.runShared(this);
    }

    @Override
    public PushTaskDefinition definition() {
        return definition;
    }

    @Override
    public BotResponse buildResponse(String groupOpenId) {
        return scriptStatusService.buildBagSpaceWarningPush();
    }
}