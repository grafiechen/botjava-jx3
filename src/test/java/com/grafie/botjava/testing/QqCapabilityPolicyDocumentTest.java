package com.grafie.botjava.testing;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class QqCapabilityPolicyDocumentTest {

    @Test
    void shouldDocumentImplementedQqPolicyBoundaries() throws IOException {
        String document = Files.readString(
                Path.of("docs", "QQ_CAPABILITY_POLICY.md"), StandardCharsets.UTF_8);

        List<String> requiredTerms = List.of(
                "GROUP_MESSAGE_CREATE",
                "GROUP_AT_MESSAGE_CREATE",
                "INTERACTION_CREATE",
                "GROUP_MSG_RECEIVE",
                "GROUP_MSG_REJECT",
                "msg_id",
                "event_id",
                "GROUP_ADMIN",
                "PRODUCTION",
                "EXPERIMENTAL",
                "REVIEW",
                "群公告 内容",
                "DeliveryMode.ACTIVE",
                "member_openid",
                "group_openid",
                "默认 5 秒",
                "默认 30 秒"
        );
        for (String term : requiredTerms) {
            assertTrue(document.contains(term), "QQ 能力政策缺少关键边界：" + term);
        }
    }
}
