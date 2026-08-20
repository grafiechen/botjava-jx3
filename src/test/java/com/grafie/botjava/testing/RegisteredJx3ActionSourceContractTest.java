package com.grafie.botjava.testing;

import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.util.REGEX;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegisteredJx3ActionSourceContractTest {

    private static final Path ACTION_SOURCE_ROOT = Path.of(
            "src", "main", "java", "com", "grafie", "botjava", "jx3", "http", "action");

    @Test
    void shouldRequireEveryRegisteredActionToOwnANonNullResponseHandler() throws IOException {
        Set<Class<? extends Jx3BaseAction>> actionTypes = new LinkedHashSet<>();
        Arrays.stream(REGEX.values()).map(REGEX::getBaseAction).forEach(actionTypes::add);

        for (Class<? extends Jx3BaseAction> actionType : actionTypes) {
            Path source = ACTION_SOURCE_ROOT.resolve(actionType.getSimpleName() + ".java");
            assertTrue(Files.isRegularFile(source), "找不到已注册 Action 源码：" + actionType.getName());
            assertNonNullResponseHandler(source);
        }
    }

    @Test
    void shouldRequireEveryConcreteActionSourceToOwnANonNullResponseHandler() throws IOException {
        try (Stream<Path> sources = Files.walk(ACTION_SOURCE_ROOT)) {
            for (Path source : sources.filter(path -> path.toString().endsWith(".java")).toList()) {
                String sourceText = Files.readString(source, StandardCharsets.UTF_8);
                if (sourceText.contains("extends Jx3BaseAction")
                        && !sourceText.contains("abstract class")) {
                    assertNonNullResponseHandler(source);
                }
            }
        }
    }

    @Test
    void shouldNotCreateActionBeanWithoutARegisteredCommand() throws IOException {
        Set<String> registeredTypes = new LinkedHashSet<>();
        Arrays.stream(REGEX.values())
                .map(REGEX::getBaseAction)
                .map(Class::getSimpleName)
                .forEach(registeredTypes::add);

        try (Stream<Path> sources = Files.list(ACTION_SOURCE_ROOT)) {
            for (Path source : sources.filter(path -> path.toString().endsWith("Action.java")).toList()) {
                String sourceText = Files.readString(source, StandardCharsets.UTF_8);
                if (sourceText.contains("@Jx3Action") && sourceText.contains("extends Jx3BaseAction")) {
                    String actionName = source.getFileName().toString().replace(".java", "");
                    assertTrue(registeredTypes.contains(actionName),
                            "Jx3Action Bean 没有对应指令：" + actionName);
                }
            }
        }
    }

    private void assertNonNullResponseHandler(Path source) throws IOException {
        String sourceText = Files.readString(source, StandardCharsets.UTF_8);
        String methodBody = methodBody(sourceText, "dealAfterJx3ApiRequest");
        assertFalse(methodBody.contains("return null;"),
                source.getFileName() + " 不允许返回 null");
    }

    private String methodBody(String source, String methodName) {
        int methodIndex = source.indexOf(methodName);
        assertTrue(methodIndex >= 0, "Action 必须显式实现 " + methodName);
        int openBrace = source.indexOf('{', methodIndex);
        assertTrue(openBrace >= 0, "无法读取 " + methodName + " 方法体");
        int depth = 0;
        for (int index = openBrace; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(openBrace + 1, index);
                }
            }
        }
        throw new AssertionError("方法体缺少结束括号：" + methodName);
    }
}
