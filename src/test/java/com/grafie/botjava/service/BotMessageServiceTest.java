package com.grafie.botjava.service;

import com.grafie.botjava.action.BaseAction;
import com.grafie.botjava.contants.OpCode;
import com.grafie.botjava.contants.PayloadTEnum;
import com.grafie.botjava.entity.dto.payload.Payload;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BotMessageServiceTest {

    @Test
    void shouldDispatchGroupMessageThroughPayloadRegistry() {
        PayloadActionRegistry registry = mock(PayloadActionRegistry.class);
        BaseAction action = mock(BaseAction.class);
        Payload payload = new Payload();
        payload.setOp(OpCode.DISPATCH.getCode());
        payload.setT(PayloadTEnum.GROUP_MESSAGE_CREATE.getValue());
        when(registry.get(PayloadTEnum.GROUP_MESSAGE_CREATE)).thenReturn(action);
        when(action.doAction(payload)).thenReturn("handled");
        BotMessageService service = new BotMessageService(registry);

        Object result = service.dealMessage(payload);

        assertEquals("handled", result);
        verify(registry).get(PayloadTEnum.GROUP_MESSAGE_CREATE);
        verify(action).doAction(payload);
    }
}
