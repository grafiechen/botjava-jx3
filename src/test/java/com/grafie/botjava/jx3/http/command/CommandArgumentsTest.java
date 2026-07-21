package com.grafie.botjava.jx3.http.command;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandArgumentsTest {

    @Test
    void shouldResolveCompatibleServerAndRoleGroups() {
        CommandArguments arguments = CommandArguments.of(Map.of(
                "server1", " 乾坤一掷 ",
                "value1", " 加菲 "
        ));

        assertEquals("乾坤一掷", arguments.server("梦江南"));
        assertEquals("加菲", arguments.roleName());
        assertEquals("加菲", arguments.value());
    }

    @Test
    void shouldUseDefaultsAndExposeReadOnlyValues() {
        CommandArguments arguments = CommandArguments.of(Map.of("value", "五行石"));

        assertEquals("梦江南", arguments.server("梦江南"));
        assertEquals("五行石", arguments.keyword());
        assertThrows(UnsupportedOperationException.class,
                () -> arguments.asMap().put("value", "修改"));
    }

    @Test
    void shouldMergeUserDefaultsWithoutReplacingExplicitArguments() {
        CommandArguments missing = CommandArguments.of(Map.of());
        CommandArguments merged = missing.withDefaults("乾坤一掷", "加菲", "万花");
        assertEquals("乾坤一掷", merged.server("梦江南"));
        assertEquals("加菲", merged.roleName());
        assertEquals("万花", merged.school());

        CommandArguments explicit = CommandArguments.of(Map.of(
                        "server", "梦江南", "value", "现用角色", "school", "七秀"))
                .withDefaults("乾坤一掷", "绑定角色", "万花");
        assertEquals("梦江南", explicit.server(null));
        assertEquals("现用角色", explicit.roleName());
        assertEquals("七秀", explicit.school());
    }

    @Test
    void shouldParseAndValidateIntegerArguments() {
        assertEquals(7, CommandArguments.of(Map.of("page", "7"))
                .integer("page", 1, 20).orElseThrow());
        assertTrue(CommandArguments.of(Map.of()).integer("page", 1, 20).isEmpty());
        assertThrows(CommandArgumentException.class,
                () -> CommandArguments.of(Map.of("page", "abc")).integer("page", 1, 20));
        assertThrows(CommandArgumentException.class,
                () -> CommandArguments.of(Map.of("page", "21")).integer("page", 1, 20));
        assertEquals(33, CommandArguments.of(Map.of("mode", "33"))
                .integerOneOf("mode", 22, 33, 55).orElseThrow());
        assertThrows(CommandArgumentException.class,
                () -> CommandArguments.of(Map.of("mode", "44")).integerOneOf("mode", 22, 33, 55));
        assertEquals(570790267L, CommandArguments.of(Map.of("uid", "570790267"))
                .longInteger("uid", 10000, 9_999_999_999L).orElseThrow());
    }
}
