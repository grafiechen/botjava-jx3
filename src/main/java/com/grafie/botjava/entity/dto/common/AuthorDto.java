package com.grafie.botjava.entity.dto.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author grafie.chen
 * @since 2025/1/22  12:36
 */
@Data
public class AuthorDto {
    private static final String ROLE_OWNER = "owner";
    private static final String ROLE_ADMIN = "admin";

    private String id;
    private String username;
    private Boolean bot;
    @JsonProperty(value = "user_openid")
    private String userOpenid;
    @JsonProperty(value = "member_openid")
    private String memberOpenid;
    @JsonProperty(value = "member_role")
    private String memberRole;
    @JsonProperty(value = "union_openid")
    private String unionOpenid;
    @JsonProperty(value = "union_user_account")
    private String unionUserAccount;

    public boolean isGroupOwner() {
        return hasMemberRole(ROLE_OWNER);
    }

    public boolean isGroupAdmin() {
        return hasMemberRole(ROLE_ADMIN);
    }

    public boolean canManageGroup() {
        return canManageGroup(memberRole);
    }

    public static boolean canManageGroup(String memberRole) {
        return ROLE_OWNER.equalsIgnoreCase(safeRole(memberRole))
                || ROLE_ADMIN.equalsIgnoreCase(safeRole(memberRole));
    }

    private boolean hasMemberRole(String role) {
        return role.equalsIgnoreCase(safeRole(memberRole));
    }

    private static String safeRole(String memberRole) {
        return memberRole == null ? "" : memberRole.trim();
    }
}
