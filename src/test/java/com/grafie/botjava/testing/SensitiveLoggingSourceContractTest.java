package com.grafie.botjava.testing;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SensitiveLoggingSourceContractTest {

    private static final Path MAIN_SOURCE = Path.of("src", "main", "java");
    private static final Set<String> FORBIDDEN_LEGACY_MAIN_SOURCE_TOKENS = Set.of(
            "SpringContextUtil",
            "BotRequestUtl",
            "先写死",
            "临时审批",
            "审核用写死"
    );

    @Test
    void shouldNotAddGroupOrMessageIdentifiersToLogTemplates() throws Exception {
        try (Stream<Path> sources = Files.walk(MAIN_SOURCE)) {
            for (Path source : sources.filter(path -> path.toString().endsWith(".java")).toList()) {
                String text = Files.readString(source, StandardCharsets.UTF_8);
                assertFalse(text.contains("groupOpenId=>"),
                        source + " 不得记录群 openid");
                assertFalse(text.contains("messageId=>"),
                        source + " 不得记录消息 id");
                assertFalse(text.contains("memberOpenId=>"),
                        source + " 不得记录成员 openid");
            }
        }
    }

    @Test
    void shouldKeepLegacyDispatchAndReviewStubsOutOfMainSources() throws Exception {
        try (Stream<Path> sources = Files.walk(MAIN_SOURCE)) {
            for (Path source : sources.filter(path -> path.toString().endsWith(".java")).toList()) {
                String text = Files.readString(source, StandardCharsets.UTF_8);
                for (String token : FORBIDDEN_LEGACY_MAIN_SOURCE_TOKENS) {
                    assertFalse(text.contains(token), source + " 不得重新引入长期设计外的临时或旧链路标记：" + token);
                }
            }
        }
    }

    @Test
    void shouldKeepQqTransportAssemblyInsideGroupMessageSender() throws Exception {
        try (Stream<Path> sources = Files.walk(MAIN_SOURCE)) {
            for (Path source : sources.filter(path -> path.toString().endsWith(".java")).toList()) {
                String text = Files.readString(source, StandardCharsets.UTF_8);
                boolean assemblesGroupMessage = text.contains("new TxMessageInfo(")
                        || text.contains("setMsg_type(")
                        || text.contains("setMsg_id(")
                        || text.contains("setMsg_seq(")
                        || text.contains("setEvent_id(");
                if (assemblesGroupMessage) {
                    assertTrue(source.endsWith(Path.of("service", "GroupMessageSender.java")),
                            source + " 不应直接拼装 QQ 群消息传输字段");
                }
            }
        }
    }
}
