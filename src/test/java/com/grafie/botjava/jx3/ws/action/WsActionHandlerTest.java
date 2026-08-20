package com.grafie.botjava.jx3.ws.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.jx3.ws.data.BaseWsData;
import com.grafie.botjava.jx3.ws.data.WsDataAction2004;
import com.grafie.botjava.jx3.ws.service.WsDataPushService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class WsActionHandlerTest {

    @BeforeAll
    static void initializeTypedDataMappings() throws Exception {
        new WsActionDataManager().init(List.of("com.grafie.botjava.jx3.ws.data"));
    }

    @Test
    void semanticDuplicateShouldHaveSameFingerprintDespiteJsonOrdering() {
        WsDataPushService pushService = mock(WsDataPushService.class);
        WsActionHandler handler = new WsActionHandler(pushService, new ObjectMapper());

        handler.pushMessage(new TextMessage(
                "{\"action\":2001,\"data\":{\"server\":\"乾坤一掷\",\"status\":1}}"));
        handler.pushMessage(new TextMessage(
                "{ \"data\": { \"status\": 1, \"server\": \"乾坤一掷\" }, \"action\": 2001 }"));

        ArgumentCaptor<BaseWsData> captor = ArgumentCaptor.forClass(BaseWsData.class);
        verify(pushService, times(2)).pushDataByWs(captor.capture());
        assertThat(captor.getAllValues().get(0).getEventFingerprint())
                .isEqualTo(captor.getAllValues().get(1).getEventFingerprint())
                .matches("[0-9a-f]{64}");
    }

    @Test
    void shouldParseAction2004DetailPayload() {
        WsDataPushService pushService = mock(WsDataPushService.class);
        WsActionHandler handler = new WsActionHandler(pushService, new ObjectMapper());
        String payload = """
                {"action":2004,"status":"success","detail":{
                  "tags":"818","zone":"-","server":"-","tieba":"剑网3",
                  "title":"【818】匹配机制的问题去找西山居别来恶心我",
                  "url":"https://tieba.baidu.com/p/10941004088","date":"2026-08-13"
                }}
                """;

        handler.pushMessage(new TextMessage(payload));

        ArgumentCaptor<BaseWsData> captor = ArgumentCaptor.forClass(BaseWsData.class);
        verify(pushService).pushDataByWs(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(WsDataAction2004.class);
        WsDataAction2004 data = (WsDataAction2004) captor.getValue();
        assertThat(data.getAction()).isEqualTo(2004);
        assertThat(data.getTags()).isEqualTo("818");
        assertThat(data.getZone()).isEqualTo("-");
        assertThat(data.getServer()).isEqualTo("-");
        assertThat(data.getTieba()).isEqualTo("剑网3");
        assertThat(data.getTitle()).contains("匹配机制");
        assertThat(data.getDate()).isEqualTo("2026-08-13");
        assertThat(data.getEventFingerprint()).matches("[0-9a-f]{64}");
    }

    @Test
    void shouldIgnorePayloadWithoutDataOrDetail() {
        WsDataPushService pushService = mock(WsDataPushService.class);
        WsActionHandler handler = new WsActionHandler(pushService, new ObjectMapper());

        handler.pushMessage(new TextMessage("{\"action\":2004,\"status\":\"success\"}"));

        verifyNoInteractions(pushService);
    }
}