package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.command.CommandArguments;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class GroupAnnouncementActionTest {

    @Test
    void shouldReturnActiveTextWithoutCallingExternalApi() {
        GroupAnnouncementAction action = new GroupAnnouncementAction(
                new ApiProperties(), mock(Jx3RequestUtil.class), mock(GroupConfigurationService.class));

        BotResponse response = action.doRequest(
                new GroupAtMessageCreateDto(),
                "群公告 今晚八点开团",
                REGEX.GroupAnnouncement,
                CommandArguments.of(Map.of("text", "今晚八点开团")));

        assertEquals(BotResponse.ResponseType.TEXT, response.getResponseType());
        assertEquals(BotResponse.DeliveryMode.ACTIVE, response.getDeliveryMode());
        assertEquals("今晚八点开团", response.getContent());
    }
}
