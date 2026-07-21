package com.grafie.botjava.action;

import com.grafie.botjava.config.BotMessageAction;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.entity.dto.payload.Payload;
import com.grafie.botjava.service.GroupCommandExecutionService;
import com.grafie.botjava.util.ObjectMapperUtil;

/**
 * @author grafie.chen
 * @since 2025/1/22  12:49
 */
@BotMessageAction
@SuppressWarnings("unchecked")
public class GroupAtMessageAction extends BaseAction {

    private final GroupCommandExecutionService commandExecutionService;

    public GroupAtMessageAction(GroupCommandExecutionService commandExecutionService) {
        this.commandExecutionService = commandExecutionService;
    }

    @Override
    protected Object deal(Payload payload) throws Exception {
        GroupAtMessageCreateDto atMessageDto = ObjectMapperUtil.readValue(payload.getD(), GroupAtMessageCreateDto.class);
        commandExecutionService.execute(atMessageDto);
        return null;
    }
}
