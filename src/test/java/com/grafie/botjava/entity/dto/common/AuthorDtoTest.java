package com.grafie.botjava.entity.dto.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthorDtoTest {

    @ParameterizedTest
    @ValueSource(strings = {"owner", "OWNER", " admin ", "ADMIN"})
    void shouldTreatOwnerAndAdminAsGroupManagers(String memberRole) {
        AuthorDto author = new AuthorDto();
        author.setMemberRole(memberRole);

        assertTrue(author.canManageGroup());
        assertTrue(AuthorDto.canManageGroup(memberRole));
    }

    @Test
    void shouldExposeSpecificGroupRoleChecks() {
        AuthorDto owner = new AuthorDto();
        owner.setMemberRole("owner");
        AuthorDto admin = new AuthorDto();
        admin.setMemberRole("admin");

        assertTrue(owner.isGroupOwner());
        assertFalse(owner.isGroupAdmin());
        assertFalse(admin.isGroupOwner());
        assertTrue(admin.isGroupAdmin());
    }

    @ParameterizedTest
    @ValueSource(strings = {"member", "guest", ""})
    void shouldRejectNonManagerRoles(String memberRole) {
        assertFalse(AuthorDto.canManageGroup(memberRole));
    }

    @Test
    void shouldRejectMissingRole() {
        assertFalse(AuthorDto.canManageGroup(null));
    }
}
