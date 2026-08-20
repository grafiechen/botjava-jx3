package com.grafie.botjava.jx3.http.action;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import com.grafie.botjava.service.SystemStatusService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SystemStatusActionTest {

    @Test
    void shouldReturnSystemStatusWithoutExternalRequest() {
        SystemStatusService service = mock(SystemStatusService.class);
        when(service.buildStatusText()).thenReturn("系统状态\n/actuator/health：可用（UP）");
        SystemStatusAction action = new SystemStatusAction(new ApiProperties(), mock(Jx3RequestUtil.class),
                mock(GroupConfigurationService.class), service);

        BotResponse response = action.doRequest(new GroupAtMessageCreateDto(), "状态检查", REGEX.SystemStatus);

        assertEquals("系统状态\n/actuator/health：可用（UP）", response.getContent());
        assertEquals(REGEX.SystemStatus, REGEX.matchEnum("状态检查"));
        assertEquals(REGEX.SystemStatus, REGEX.matchEnum("/状态检查"));
    }
}
