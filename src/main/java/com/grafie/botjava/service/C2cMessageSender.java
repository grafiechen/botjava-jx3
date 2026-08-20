package com.grafie.botjava.service;

import com.grafie.botjava.entity.dto.c2c.C2cMessageCreateDto;
import com.grafie.botjava.entity.dto.common.TxMessageInfo;
import org.springframework.stereotype.Service;

@Service
public class C2cMessageSender {

    private final QqUserMessageClient userMessageClient;

    public C2cMessageSender(QqUserMessageClient userMessageClient) {
        this.userMessageClient = userMessageClient;
    }

    public void replyText(C2cMessageCreateDto sourceMessage, String content) {
        if (sourceMessage == null || sourceMessage.userOpenId() == null || sourceMessage.userOpenId().isBlank()) {
            throw new IllegalArgumentException("C2C source message userOpenId must not be blank");
        }
        TxMessageInfo reply = new TxMessageInfo();
        reply.setMsg_type(0);
        reply.setContent(content);
        reply.setMsg_id(sourceMessage.getId());
        reply.setMsg_seq(1);
        userMessageClient.send(sourceMessage.userOpenId(), reply);
    }
}