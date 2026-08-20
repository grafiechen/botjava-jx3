package com.grafie.botjava.service;

import com.grafie.botjava.config.QqAdminProperties;
import com.grafie.botjava.entity.dto.common.AuthorDto;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.http.util.REGEX;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class NoopAppearanceNamePermissionConfiguration implements AppearanceNamePermissionConfiguration {

    private final QqAdminProperties adminProperties;
    private final GroupCommandPermissionConfiguration permissionConfiguration;

    public NoopAppearanceNamePermissionConfiguration(QqAdminProperties adminProperties,
                                                     GroupCommandPermissionConfiguration permissionConfiguration) {
        this.adminProperties = adminProperties;
        this.permissionConfiguration = permissionConfiguration;
    }

    @Override
    public boolean canManageAppearanceName(GroupAtMessageCreateDto message, REGEX command) {
        if (command == null) {
            return false;
        }
        if (!requiresConfiguredPermission(command)) {
            return true;
        }
        AuthorDto author = message == null ? null : message.getAuthor();
        if (isMasterAuthor(author)) {
            return true;
        }
        String groupOpenId = message == null ? null : message.getGroupOpenid();
        String memberOpenId = author == null ? null : author.getMemberOpenid();
        Optional<GroupCommandPermissionConfiguration.Decision> decision =
                permissionConfiguration.evaluate(groupOpenId, memberOpenId, command);
        return decision.map(GroupCommandPermissionConfiguration.Decision::allowed).orElse(false);
    }

    @Override
    public boolean isAppearanceNameReviewPassed(GroupAtMessageCreateDto message, REGEX command) {
        return true;
    }

    private boolean requiresConfiguredPermission(REGEX command) {
        return switch (command) {
            case AppearanceNameAliasPendingList, AppearanceNameAliasApprove, AppearanceNameAliasDelete -> true;
            default -> false;
        };
    }

    private boolean isMasterAuthor(AuthorDto author) {
        if (author == null) {
            return false;
        }
        return adminProperties.isMasterOpenid(author.getUserOpenid())
                || adminProperties.isMasterOpenid(author.getMemberOpenid())
                || adminProperties.isMasterOpenid(author.getUnionOpenid())
                || adminProperties.isMasterOpenid(author.getId());
    }
}