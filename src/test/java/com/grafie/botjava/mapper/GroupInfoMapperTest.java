package com.grafie.botjava.mapper;

import com.grafie.botjava.entity.GroupInfo;
import com.grafie.botjava.entity.UserInfo;
import com.grafie.botjava.entity.UserRoleBinding;
import com.grafie.botjava.service.GroupConfigurationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DataJpaTest(properties = {
        "tx.bot.openapi-url=https://example.test",
        "tx.bot.access-token-url=https://example.test/token",
        "tx.bot.app-id=test-app",
        "tx.bot.app-secret=test-secret",
        "jx3api.api.api-token=test-token",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Import(GroupConfigurationService.class)
class GroupInfoMapperTest {

    @Autowired
    private GroupInfoMapper groupInfoMapper;

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private UserRoleBindingMapper userRoleBindingMapper;

    @Autowired
    private GroupConfigurationService groupConfigurationService;

    @Test
    void shouldReserveWindowAtomicallyAndRollbackOnlyMatchingReservation() {
        GroupInfo group = new GroupInfo();
        group.setOpenGroupId("group-1");
        group.setActiveMessagesEnabled(true);
        group.setActiveMessagesPlatformAllowed(true);
        groupInfoMapper.saveAndFlush(group);

        Instant firstTime = Instant.parse("2026-07-13T06:00:00Z");
        assertEquals(1, groupInfoMapper.reserveActiveMessage(
                "group-1", firstTime, firstTime.minusSeconds(60), "reservation-1"));
        assertEquals(0, groupInfoMapper.reserveActiveMessage(
                "group-1", firstTime.plusSeconds(30), firstTime.minusSeconds(30), "reservation-2"));

        assertEquals(0, groupInfoMapper.rollbackActiveMessage("group-1", "reservation-2"));
        assertEquals(1, groupInfoMapper.rollbackActiveMessage("group-1", "reservation-1"));

        GroupInfo reloaded = groupInfoMapper.findByOpenGroupId("group-1");
        assertNull(reloaded.getActiveMessageReservedAt());
        assertNull(reloaded.getActiveMessageReservationId());
    }

    @Test
    void shouldRequireLocalSwitchAndPlatformAuthorizationForActiveMessage() {
        Instant now = Instant.parse("2026-07-13T06:00:00Z");

        GroupInfo localOnly = new GroupInfo();
        localOnly.setOpenGroupId("group-local-only");
        localOnly.setActiveMessagesEnabled(true);
        localOnly.setActiveMessagesPlatformAllowed(false);
        groupInfoMapper.save(localOnly);

        GroupInfo platformOnly = new GroupInfo();
        platformOnly.setOpenGroupId("group-platform-only");
        platformOnly.setActiveMessagesEnabled(false);
        platformOnly.setActiveMessagesPlatformAllowed(true);
        groupInfoMapper.saveAndFlush(platformOnly);

        assertEquals(0, groupInfoMapper.reserveActiveMessage(
                "group-local-only", now, now.minusSeconds(60), "local-only"));
        assertEquals(0, groupInfoMapper.reserveActiveMessage(
                "group-platform-only", now, now.minusSeconds(60), "platform-only"));
    }

    @Test
    void shouldKeepGroupServerIsolatedAndAccountRoleSharedAcrossGroups() {
        GroupInfo firstGroup = new GroupInfo();
        firstGroup.setOpenGroupId("group-1");
        firstGroup.setServer("乾坤一掷");
        groupInfoMapper.save(firstGroup);

        GroupInfo secondGroup = new GroupInfo();
        secondGroup.setOpenGroupId("group-2");
        secondGroup.setServer("梦江南");
        groupInfoMapper.save(secondGroup);

        UserInfo account = new UserInfo();
        account.setMemberOpenId("account-1");
        account.setServer("唯我独尊");
        account.setRoleName("加菲");
        userInfoMapper.save(account);

        UserRoleBinding role = new UserRoleBinding();
        role.setGroupOpenId("group-1");
        role.setMemberOpenId("account-1");
        role.setServer("唯我独尊");
        role.setRoleName("加菲");
        userRoleBindingMapper.saveAndFlush(role);

        assertEquals("乾坤一掷", groupInfoMapper.findByOpenGroupId("group-1").getServer());
        assertEquals("梦江南", groupInfoMapper.findByOpenGroupId("group-2").getServer());
        assertEquals("加菲", userInfoMapper.findByMemberOpenId("account-1").getRoleName());
        assertEquals(1, userRoleBindingMapper.findByGroupOpenIdAndMemberOpenIdOrderByIdAsc("group-1", "account-1").size());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldPersistWholeGroupConfigurationThroughServiceWithoutOuterTransaction() {
        GroupConfigurationService.BindServerResult first =
                groupConfigurationService.bindServer("service-group-1", "乾坤一掷");
        GroupConfigurationService.BindServerResult second =
                groupConfigurationService.bindServer("service-group-2", "梦江南");
        GroupConfigurationService.BindServerResult duplicate =
                groupConfigurationService.bindServer("service-group-1", "唯我独尊");
        groupConfigurationService.setEnabled(
                "service-group-1", GroupConfigurationService.Setting.EXPERIMENTAL, true);

        assertEquals(true, first.bound());
        assertEquals(true, second.bound());
        assertEquals(false, duplicate.bound());
        assertEquals("乾坤一掷", duplicate.server());
        assertEquals("乾坤一掷", groupInfoMapper.findByOpenGroupId("service-group-1").getServer());
        assertEquals("梦江南", groupInfoMapper.findByOpenGroupId("service-group-2").getServer());
        assertEquals(true, groupInfoMapper.findByOpenGroupId("service-group-1").getExperimentalEnabled());
    }
}
