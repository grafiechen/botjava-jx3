package com.grafie.botjava.tx;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.grafie.botjava.action.GroupAtMessageAction;
import com.grafie.botjava.entity.dto.payload.Payload;
import com.grafie.botjava.util.BotRequestUtl;
import com.grafie.botjava.util.ObjectMapperUtil;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BotTxTests {
    @Resource
    private BotRequestUtl botRequestUtl;

    @Test
    void accessTokenTest() {
        botRequestUtl.refreshToken();
    }


    @Test
    void groupAtMessageTest() throws JsonProcessingException {
        String message = "{\n" +
                "    \"op\": 0,\n" +
                "    \"s\": null,\n" +
                "    \"d\": {\n" +
                "        \"id\": \"ROBOT1.0_MBHA3DliTc-RB9rsyzfE.66.o!\",\n" +
                "        \"content\": \"日常 乾坤一掷 0\",\n" +
                "        \"timestamp\": \"2025-01-22T12:27:03+08:00\",\n" +
                "        \"author\": {\n" +
                "            \"id\": \"55\",\n" +
                "            \"member_openid\": \"44\",\n" +
                "            \"union_openid\": \"33\"\n" +
                "        },\n" +
                "        \"group_id\": \"22,\n" +
                "        \"group_openid\": \"11\",\n" +
                "        \"message_scene\": {\n" +
                "            \"source\": \"default\",\n" +
                "            \"callback_data\": null\n" +
                "        }\n" +
                "    },\n" +
                "    \"t\": \"GROUP_AT_MESSAGE_CREATE\"\n" +
                "}";
        GroupAtMessageAction action = new GroupAtMessageAction(botRequestUtl);
        action.doAction(ObjectMapperUtil.readValue(message, Payload.class));
    }
}
