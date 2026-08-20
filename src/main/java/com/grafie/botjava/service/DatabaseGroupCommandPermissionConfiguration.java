package com.grafie.botjava.service;

import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.mapper.GroupCommandPermissionGrantMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service
public class DatabaseGroupCommandPermissionConfiguration implements GroupCommandPermissionConfiguration {
    public static final String DAILY_PUSH_FIELD_CONFIG = "DAILY_PUSH_FIELD_CONFIG";
    private static final Set<REGEX> DAILY_PUSH_COMMANDS = Set.of(
            REGEX.DailyPushFieldAdd, REGEX.DailyPushFieldDelete);

    private final GroupCommandPermissionGrantMapper mapper;

    public DatabaseGroupCommandPermissionConfiguration(GroupCommandPermissionGrantMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean isPermissionCommand(REGEX command) {
        return DAILY_PUSH_COMMANDS.contains(command);
    }

    @Override
    public Optional<Decision> evaluate(String groupOpenId, String memberOpenId, REGEX command) {
        if (!isPermissionCommand(command)) {
            return Optional.empty();
        }
        if (clean(groupOpenId) == null || clean(memberOpenId) == null) {
            return Optional.of(Decision.DENY);
        }
        boolean allowed = mapper.existsByGroupOpenIdAndMemberOpenIdAndPermissionKeyAndEnabledTrue(
                groupOpenId.trim(), memberOpenId.trim(), DAILY_PUSH_FIELD_CONFIG)
                || mapper.existsByGroupOpenIdAndMemberOpenIdAndPermissionKeyAndEnabledTrue(
                "*", memberOpenId.trim(), DAILY_PUSH_FIELD_CONFIG);
        return Optional.of(allowed ? Decision.ALLOW : Decision.DENY);
    }

    private String clean(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}