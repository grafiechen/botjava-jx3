package com.grafie.botjava.action;

import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.entity.dto.payload.Payload;
import com.grafie.botjava.service.GroupCommandExecutionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GroupAtMessageActionTest {

    @Test
    void shouldParseUtf8GroupPayloadAndDelegateExecution() throws Exception {
        GroupCommandExecutionService executionService = mock(GroupCommandExecutionService.class);
        GroupAtMessageAction action = new GroupAtMessageAction(executionService);

        action.doAction(payload());

        ArgumentCaptor<GroupAtMessageCreateDto> messageCaptor =
                ArgumentCaptor.forClass(GroupAtMessageCreateDto.class);
        verify(executionService).execute(messageCaptor.capture());
        assertEquals("/开服 乾坤一掷", messageCaptor.getValue().getContent());
        assertEquals("group-1", messageCaptor.getValue().getGroupOpenid());
        assertEquals("user-1", messageCaptor.getValue().getAuthor().getMemberOpenid());
        assertEquals("member", messageCaptor.getValue().getAuthor().getMemberRole());
    }

    @Test
    void shouldLetBaseActionHandleExecutionFailure() throws Exception {
        GroupCommandExecutionService executionService = mock(GroupCommandExecutionService.class);
        doThrow(new IllegalStateException("execution failed token=secret"))
                .when(executionService).execute(org.mockito.ArgumentMatchers.any());
        GroupAtMessageAction action = new GroupAtMessageAction(executionService);

        assertNull(action.doAction(payload()));
        verify(executionService).execute(org.mockito.ArgumentMatchers.any());
    }

    private static Payload payload() {
        Payload payload = new Payload();
        payload.setOp(0);
        payload.setT("GROUP_MESSAGE_CREATE");
        payload.setD(Map.of(
                "id", "message-1",
                "content", "/开服 乾坤一掷",
                "timestamp", "2026-07-13T09:30:00+09:00",
                "author", Map.of(
                        "id", "user-1",
                        "username", "测试用户",
                        "bot", false,
                        "member_openid", "user-1",
                        "member_role", "member",
                        "union_openid", "user-1"
                ),
                "group_id", "group-1",
                "group_openid", "group-1",
                "message_scene", Map.of("source", "default", "ext", Map.of()),
                "message_type", 0
        ));
        return payload;
    }
}
