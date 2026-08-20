package com.grafie.botjava.service;

import com.grafie.botjava.entity.dto.qq.QqGroupJoinApprovalRequestDto;
import com.grafie.botjava.entity.dto.qq.QqGroupJoinRequestListDto;
import com.grafie.botjava.entity.dto.qq.QqGroupMemberMuteRequestDto;
import com.grafie.botjava.entity.dto.qq.QqGroupRestrictChatSettingDto;
import com.grafie.botjava.entity.dto.qq.QqJoinApprovalStrategyListDto;
import com.grafie.botjava.entity.dto.qq.QqJoinApprovalStrategyRequestDto;
import com.grafie.botjava.entity.dto.qq.QqJoinApprovalStrategyResultDto;
import com.grafie.botjava.entity.dto.qq.QqJoinApprovalStrategyUpdateRequestDto;
import com.grafie.botjava.entity.dto.qq.QqJoinApprovalStrategyWhitelistRequestDto;
import com.grafie.botjava.entity.dto.qq.QqJoinApprovalStrategyWhitelistResultDto;
import com.grafie.botjava.observability.BotMetrics;
import com.grafie.botjava.qq.QqOpenApiClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QqGroupManagementClientTest {

    @Test
    void shouldFetchJoinRequestsWithPagingQuery() {
        QqOpenApiClient openApiClient = mock(QqOpenApiClient.class);
        QqGroupJoinRequestListDto response = new QqGroupJoinRequestListDto();
        response.setNextCursor("cursor-next");
        when(openApiClient.get(eq("/v2/groups/group-1/join_request_list"), anyMap(),
                eq(QqGroupJoinRequestListDto.class))).thenReturn(response);
        QqGroupMessageClient client = new QqGroupMessageClient(openApiClient, mock(BotMetrics.class));

        assertEquals("cursor-next", client.listJoinRequests(" group-1 ", " cursor-1 ", 50).getNextCursor());

        ArgumentCaptor<Map<String, Object>> queryCaptor = ArgumentCaptor.forClass(Map.class);
        verify(openApiClient).get(eq("/v2/groups/group-1/join_request_list"), queryCaptor.capture(),
                eq(QqGroupJoinRequestListDto.class));
        assertEquals("cursor-1", queryCaptor.getValue().get("cursor"));
        assertEquals(50, queryCaptor.getValue().get("limit"));
    }

    @Test
    void shouldApproveJoinRequestOnOfficialPath() {
        QqOpenApiClient openApiClient = mock(QqOpenApiClient.class);
        QqGroupMessageClient client = new QqGroupMessageClient(openApiClient, mock(BotMetrics.class));

        client.approveJoinRequest("group-1", " member-1 ", QqGroupJoinApprovalRequestDto.approve("join-1"));

        ArgumentCaptor<Map<String, Object>> bodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(openApiClient).post(eq("/v2/groups/group-1/approval_join_request/member-1"),
                bodyCaptor.capture(), eq(String.class));
        assertEquals("approve", bodyCaptor.getValue().get("op"));
        assertEquals("join-1", bodyCaptor.getValue().get("join_request_id"));
    }

    @Test
    void shouldDeclineJoinRequestWithReasonAndBlacklistFlag() {
        QqOpenApiClient openApiClient = mock(QqOpenApiClient.class);
        QqGroupMessageClient client = new QqGroupMessageClient(openApiClient, mock(BotMetrics.class));

        client.approveJoinRequest("group-1", "member-1",
                QqGroupJoinApprovalRequestDto.decline("join-1", "not allowed", true));

        ArgumentCaptor<Map<String, Object>> bodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(openApiClient).post(eq("/v2/groups/group-1/approval_join_request/member-1"),
                bodyCaptor.capture(), eq(String.class));
        assertEquals("decline", bodyCaptor.getValue().get("op"));
        assertEquals("join-1", bodyCaptor.getValue().get("join_request_id"));
        assertEquals("not allowed", bodyCaptor.getValue().get("reject_reason"));
        assertEquals(true, bodyCaptor.getValue().get("add_to_member_blacklist"));
    }

    @Test
    void shouldFetchAndUpdateRestrictChatSetting() {
        QqOpenApiClient openApiClient = mock(QqOpenApiClient.class);
        QqGroupRestrictChatSettingDto response = new QqGroupRestrictChatSettingDto();
        when(openApiClient.get(eq("/v2/groups/group-1/restrict_chat_setting"), eq(Map.of()),
                eq(QqGroupRestrictChatSettingDto.class))).thenReturn(response);
        QqGroupMessageClient client = new QqGroupMessageClient(openApiClient, mock(BotMetrics.class));
        QqGroupMemberMuteRequestDto muteRequest = QqGroupMemberMuteRequestDto.of(List.of(
                QqGroupMemberMuteRequestDto.SetMemberMuteState.add("member-1", "2026-08-11T22:00:00+08:00")));

        assertEquals(response, client.getRestrictChatSetting("group-1"));
        client.updateRestrictChatSetting("group-1", muteRequest);

        ArgumentCaptor<Map<String, Object>> bodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(openApiClient).post(eq("/v2/groups/group-1/restrict_chat_setting"), bodyCaptor.capture(),
                eq(String.class));
        List<Map<String, Object>> members = (List<Map<String, Object>>) bodyCaptor.getValue().get("members");
        assertEquals("add", members.get(0).get("op"));
        assertEquals("member-1", members.get(0).get("member_openid"));
        assertEquals("2026-08-11T22:00:00+08:00", members.get(0).get("mute_expire_at"));
    }

    @Test
    void shouldManageJoinApprovalStrategies() {
        QqOpenApiClient openApiClient = mock(QqOpenApiClient.class);
        QqJoinApprovalStrategyListDto listResponse = new QqJoinApprovalStrategyListDto();
        QqJoinApprovalStrategyResultDto result = new QqJoinApprovalStrategyResultDto();
        result.setStrategyId("strategy-1");
        QqJoinApprovalStrategyWhitelistResultDto whitelistResult = new QqJoinApprovalStrategyWhitelistResultDto();
        whitelistResult.setWhitelistUserCount(2);
        when(openApiClient.get(eq("/v2/groups/join_approval_strategy"), anyMap(),
                eq(QqJoinApprovalStrategyListDto.class))).thenReturn(listResponse);
        when(openApiClient.post(eq("/v2/groups/join_approval_strategy"), anyMap(),
                eq(QqJoinApprovalStrategyResultDto.class))).thenReturn(result);
        when(openApiClient.patch(eq("/v2/groups/join_approval_strategy/strategy-1"), anyMap(),
                eq(QqJoinApprovalStrategyResultDto.class))).thenReturn(result);
        when(openApiClient.post(eq("/v2/groups/join_approval_strategy/strategy-1/whitelist_users"), anyMap(),
                eq(QqJoinApprovalStrategyWhitelistResultDto.class))).thenReturn(whitelistResult);
        QqGroupMessageClient client = new QqGroupMessageClient(openApiClient, mock(BotMetrics.class));

        QqJoinApprovalStrategyRequestDto createRequest = new QqJoinApprovalStrategyRequestDto();
        createRequest.setGroupOpenids(List.of("group-1"));
        createRequest.setIsEnable("on");
        QqJoinApprovalStrategyUpdateRequestDto updateRequest = new QqJoinApprovalStrategyUpdateRequestDto();
        updateRequest.setIsEnable("off");
        QqJoinApprovalStrategyWhitelistRequestDto whitelistRequest = new QqJoinApprovalStrategyWhitelistRequestDto();
        whitelistRequest.setOp("add");
        whitelistRequest.setWhitelistUsers(List.of("1234567", "1234568"));

        assertEquals(listResponse, client.listJoinApprovalStrategies("cursor-1", 20));
        assertEquals("strategy-1", client.createJoinApprovalStrategy(createRequest).getStrategyId());
        assertEquals("strategy-1", client.updateJoinApprovalStrategy("strategy-1", updateRequest).getStrategyId());
        client.deleteJoinApprovalStrategy("strategy-1");
        client.executeJoinApprovalStrategy("strategy-1");
        assertEquals(2, client.updateJoinApprovalStrategyWhitelistUsers("strategy-1", whitelistRequest)
                .getWhitelistUserCount());

        ArgumentCaptor<Map<String, Object>> listQueryCaptor = ArgumentCaptor.forClass(Map.class);
        verify(openApiClient).get(eq("/v2/groups/join_approval_strategy"), listQueryCaptor.capture(),
                eq(QqJoinApprovalStrategyListDto.class));
        assertEquals("cursor-1", listQueryCaptor.getValue().get("cursor"));
        assertEquals(20, listQueryCaptor.getValue().get("limit"));
        verify(openApiClient).delete(eq("/v2/groups/join_approval_strategy/strategy-1"), eq(Map.of()), eq(String.class));
        verify(openApiClient).post(eq("/v2/groups/join_approval_strategy/strategy-1/execute"), eq(Map.of()), eq(String.class));
    }
}