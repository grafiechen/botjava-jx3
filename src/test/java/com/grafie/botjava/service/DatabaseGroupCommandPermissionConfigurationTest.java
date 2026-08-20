package com.grafie.botjava.service;

import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.mapper.GroupCommandPermissionGrantMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DatabaseGroupCommandPermissionConfigurationTest {
    @Test
    void shouldOnlyAuthorizeDatabaseGrantedDailyPushConfiguration() {
        GroupCommandPermissionGrantMapper mapper = mock(GroupCommandPermissionGrantMapper.class);
        when(mapper.existsByGroupOpenIdAndMemberOpenIdAndPermissionKeyAndEnabledTrue(
                "group-1", "member-1", DatabaseGroupCommandPermissionConfiguration.DAILY_PUSH_FIELD_CONFIG))
                .thenReturn(true);
        DatabaseGroupCommandPermissionConfiguration configuration =
                new DatabaseGroupCommandPermissionConfiguration(mapper);

        assertThat(configuration.isPermissionCommand(REGEX.DailyPushFieldAdd)).isTrue();
        assertThat(configuration.isPermissionCommand(REGEX.DailyPushFieldList)).isFalse();
        assertThat(configuration.evaluate("group-1", "member-1", REGEX.DailyPushFieldAdd))
                .contains(GroupCommandPermissionConfiguration.Decision.ALLOW);
        assertThat(configuration.evaluate("group-1", "member-2", REGEX.DailyPushFieldDelete))
                .contains(GroupCommandPermissionConfiguration.Decision.DENY);
        assertThat(configuration.evaluate("group-1", "member-1", REGEX.ServerCheck)).isEmpty();
    }
}