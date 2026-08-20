package com.grafie.botjava.jx3.ws.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.jx3.ws.data.WsDataAction2004;
import com.grafie.botjava.service.push.GroupPushDispatcher;
import com.grafie.botjava.service.push.PushTaskDefinition;
import com.grafie.botjava.service.push.PushTaskRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WsDataPushServiceTest {

    @Test
    void shouldBuildReadableAction2004MessageWithoutExternalUrl() {
        GroupPushDispatcher dispatcher = mock(GroupPushDispatcher.class);
        WsDataPushService service = new WsDataPushService(
                new PushTaskRegistry(), dispatcher, new ObjectMapper());
        WsDataAction2004 data = new WsDataAction2004();
        data.setAction(2004);
        data.setEventFingerprint("a".repeat(64));
        data.setTags("818");
        data.setTieba("剑网3");
        data.setTitle("【818】匹配机制的问题去找西山居别来恶心我");
        data.setUrl("https://tieba.baidu.com/p/10941004088");
        data.setDate("2026-08-13");

        service.pushDataByWs(data);

        ArgumentCaptor<PushTaskDefinition> task = ArgumentCaptor.forClass(PushTaskDefinition.class);
        ArgumentCaptor<BotResponse> response = ArgumentCaptor.forClass(BotResponse.class);
        verify(dispatcher).publishWsEvent(task.capture(),
                org.mockito.ArgumentMatchers.eq("a".repeat(64)), response.capture());
        assertThat(task.getValue().code()).isEqualTo("JX3_WS_2004");
        assertThat(response.getValue().getContent())
                .contains("【JX3API推送】八卦速报")
                .contains("分类：818")
                .contains("贴吧：剑网3")
                .contains("标题：【818】匹配机制")
                .contains("日期：2026-08-13")
                .doesNotContain("tieba.baidu.com");
    }
}