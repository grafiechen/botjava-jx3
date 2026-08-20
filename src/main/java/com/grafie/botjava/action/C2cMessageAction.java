package com.grafie.botjava.action;

import com.grafie.botjava.config.BotMessageAction;
import com.grafie.botjava.entity.dto.c2c.C2cMessageCreateDto;
import com.grafie.botjava.entity.dto.payload.Payload;
import com.grafie.botjava.service.C2cAdminConfigService;
import com.grafie.botjava.util.ObjectMapperUtil;

@BotMessageAction
public class C2cMessageAction extends BaseAction {

    private final C2cAdminConfigService adminConfigService;

    public C2cMessageAction(C2cAdminConfigService adminConfigService) {
        this.adminConfigService = adminConfigService;
    }

    @Override
    protected Object deal(Payload payload) throws Exception {
        C2cMessageCreateDto message = ObjectMapperUtil.readValue(payload.getD(), C2cMessageCreateDto.class);
        adminConfigService.handle(message);
        return null;
    }
}