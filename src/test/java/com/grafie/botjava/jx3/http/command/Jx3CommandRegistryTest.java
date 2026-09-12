package com.grafie.botjava.jx3.http.command;

import com.grafie.botjava.jx3.config.ApiProperties;
import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.util.Jx3RequestUtil;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupConfigurationService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class Jx3CommandRegistryTest {

    @Test
    void shouldResolveNormalizedCommandAndItsHandler() throws Exception {
        Jx3CommandRegistry registry = new Jx3CommandRegistry(allRegisteredActions());

        ResolvedJx3Command command = registry.resolve(" /开服 乾坤一掷 ").orElseThrow();

        assertEquals("开服 乾坤一掷", command.command());
        assertEquals(REGEX.ServerCheck, command.definition());
        assertEquals("乾坤一掷", command.arguments().get("server"));
        assertEquals(REGEX.ServerCheck.getBaseAction(), command.action().getClass());
    }

    @Test
    void shouldReturnEmptyForUnknownCommand() throws Exception {
        Jx3CommandRegistry registry = new Jx3CommandRegistry(allRegisteredActions());

        assertTrue(registry.resolve("不存在的指令").isEmpty());
    }

    @Test
    void shouldResolveCommandAlias() throws Exception {
        Jx3CommandRegistry registry = new Jx3CommandRegistry(allRegisteredActions());

        ResolvedJx3Command command = registry.resolve("/开服状态 乾坤一掷").orElseThrow();

        assertEquals(REGEX.ServerCheck, command.definition());
        assertEquals("乾坤一掷", command.arguments().get("server"));
    }

    @Test
    void shouldResolveUserBindingAndRoleQueryWithoutArguments() throws Exception {
        Jx3CommandRegistry registry = new Jx3CommandRegistry(allRegisteredActions());

        ResolvedJx3Command binding = registry.resolve("/绑定角色 乾坤一掷 加菲").orElseThrow();
        assertEquals(REGEX.BindRole, binding.definition());
        assertEquals("乾坤一掷", binding.arguments().server(null));
        assertEquals("加菲", binding.arguments().roleName());
        assertEquals(null, binding.arguments().school());
        assertTrue(registry.resolve("绑定角色 乾坤一掷 加菲 万花").isEmpty());

        ResolvedJx3Command roleQuery = registry.resolve("装备").orElseThrow();
        assertEquals(REGEX.RoleAttribute, roleQuery.definition());
        assertTrue(roleQuery.definition().requiresRoleName());
        assertEquals(null, roleQuery.arguments().roleName());
    }

    @Test
    void shouldResolveAdministrativeGroupAnnouncement() throws Exception {
        Jx3CommandRegistry registry = new Jx3CommandRegistry(allRegisteredActions());

        ResolvedJx3Command command = registry.resolve("/群公告 今晚八点开团").orElseThrow();

        assertEquals(REGEX.GroupAnnouncement, command.definition());
        assertEquals("今晚八点开团", command.arguments().get("text"));
        assertTrue(command.definition().getCommandAccess().allows("owner"));
        assertFalse(command.definition().getCommandAccess().allows("member"));
        assertEquals(5, command.definition().getDefaultCooldownSeconds());
    }

    @Test
    void shouldResolveScriptStatusCommands() throws Exception {
        Jx3CommandRegistry registry = new Jx3CommandRegistry(allRegisteredActions());

        ResolvedJx3Command status = registry.resolve("脚本状态").orElseThrow();
        ResolvedJx3Command fieldQuery = registry.resolve("/查询信息 乾坤一掷 加菲 侠行点").orElseThrow();
        ResolvedJx3Command allFieldQuery = registry.resolve("/查询全部信息 背包剩余空间").orElseThrow();
        ResolvedJx3Command update = registry.resolve("脚本设置 乾坤一掷 加菲 秘籍 完成").orElseThrow();
        ResolvedJx3Command bagWarning = registry.resolve("/背包预警").orElseThrow();

        assertEquals(REGEX.ScriptStatus, status.definition());
        assertEquals(REGEX.RoleFieldQuery, fieldQuery.definition());
        assertEquals("乾坤一掷", fieldQuery.arguments().server(null));
        assertEquals("加菲", fieldQuery.arguments().roleName());
        assertEquals("侠行点", fieldQuery.arguments().get("name"));
        assertEquals(REGEX.AllRoleFieldQuery, allFieldQuery.definition());
        assertEquals("背包剩余空间", allFieldQuery.arguments().get("name"));
        assertTrue(allFieldQuery.definition().getCommandAccess().allows("member"));
        assertFalse(allFieldQuery.definition().requiresRoleName());
        assertTrue(allFieldQuery.definition().usesExternalCall());
        assertEquals(REGEX.ScriptStatusUpdate, update.definition());
        assertEquals("乾坤一掷", update.arguments().server(null));
        assertEquals("加菲", update.arguments().roleName());
        assertEquals("秘籍", update.arguments().get("name"));
        assertEquals("完成", update.arguments().get("text"));
        assertEquals(REGEX.BagSpaceWarning, bagWarning.definition());
        assertTrue(bagWarning.definition().getCommandAccess().allows("member"));
        assertFalse(bagWarning.definition().requiresRoleName());
        assertTrue(bagWarning.definition().usesExternalCall());
        assertTrue(update.definition().usesExternalCall());
        assertEquals(30, update.definition().getDefaultCooldownSeconds());
    }
    @Test
    void shouldGenerateGroupedHelpFromCommandMetadata() {
        assertTrue(REGEX.values().length > 0);
        String help = REGEX.buildHelpText();

        assertTrue(help.contains("【基础】"));
        assertTrue(help.contains("【免费查询】"));
        assertTrue(help.contains("【会员查询】"));
        assertTrue(help.contains("开服状态：开服 乾坤一掷"));
        assertFalse(help.contains("(?<server>"));
    }

    @Test
    void shouldRestrictOnlyAdministrativeCommands() {
        assertTrue(REGEX.ServerCheck.getCommandAccess().allows("member"));
        assertTrue(REGEX.BindServerCalendar.getCommandAccess().allows("owner"));
        assertTrue(REGEX.BindServerCalendar.getCommandAccess().allows("admin"));
        assertFalse(REGEX.BindServerCalendar.getCommandAccess().allows("member"));
        assertFalse(REGEX.BindServerCalendar.getCommandAccess().allows(null));
    }

    @Test
    void shouldTreatDatabaseBindingCommandsAsLocalCommands() {
        assertEquals(5, REGEX.BindServerCalendar.getDefaultCooldownSeconds());
        assertEquals(5, REGEX.BindRole.getDefaultCooldownSeconds());
        assertEquals(5, REGEX.AddRole.getDefaultCooldownSeconds());
        assertEquals(5, REGEX.ModifyRole.getDefaultCooldownSeconds());
        assertFalse(REGEX.BindServerCalendar.usesExternalCall());
        assertFalse(REGEX.BindRole.usesExternalCall());
    }

    @Test
    void shouldRejectRegistryWithMissingHandlers() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> new Jx3CommandRegistry(List.of())
        );

        assertTrue(exception.getMessage().contains("缺少处理器"));
    }

    private static List<Jx3BaseAction> allRegisteredActions() {
        ApiProperties properties = new ApiProperties();
        Jx3RequestUtil requestUtil = mock(Jx3RequestUtil.class);
        GroupConfigurationService groupConfigurationService = mock(GroupConfigurationService.class);
        return Arrays.stream(REGEX.values())
                .map(REGEX::getBaseAction)
                .distinct()
                .map(type -> instantiate(type, properties, requestUtil, groupConfigurationService))
                .toList();
    }

    private static Jx3BaseAction instantiate(
            Class<? extends Jx3BaseAction> type,
            ApiProperties properties,
            Jx3RequestUtil requestUtil,
            GroupConfigurationService groupConfigurationService
    ) {
        try {
            @SuppressWarnings("unchecked")
            Constructor<? extends Jx3BaseAction> constructor =
                    (Constructor<? extends Jx3BaseAction>) type.getConstructors()[0];
            Object[] arguments = Arrays.stream(constructor.getParameterTypes())
                    .map(parameterType -> constructorArgument(
                            parameterType, properties, requestUtil, groupConfigurationService))
                    .toArray();
            return constructor.newInstance(arguments);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("无法构造测试 Action：" + type.getName(), e);
        }
    }

    private static Object constructorArgument(
            Class<?> parameterType,
            ApiProperties properties,
            Jx3RequestUtil requestUtil,
            GroupConfigurationService groupConfigurationService
    ) {
        if (parameterType == ApiProperties.class) {
            return properties;
        }
        if (parameterType == Jx3RequestUtil.class) {
            return requestUtil;
        }
        if (parameterType == GroupConfigurationService.class) {
            return groupConfigurationService;
        }
        return mock(parameterType);
    }
}
