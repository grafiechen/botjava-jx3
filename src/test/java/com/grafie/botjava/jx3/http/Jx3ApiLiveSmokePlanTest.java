package com.grafie.botjava.jx3.http;

import com.grafie.botjava.jx3.http.util.REGEX;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class Jx3ApiLiveSmokePlanTest {

    @Test
    void shouldBuildOneResolvableCommandForEveryNonVoiceOfficialContract() throws Exception {
        List<Jx3ApiLiveSmokePlan.SmokeCase> cases = Jx3ApiLiveSmokePlan.build(
                "测试区服", "测试角色", "123456789");

        assertEquals(Jx3ApiLiveSmokePlan.officialContractCount() - 1, cases.size());
        assertEquals(cases.size(), cases.stream()
                .map(item -> item.definition().getMethodEnum())
                .distinct()
                .count());
        for (Jx3ApiLiveSmokePlan.SmokeCase item : cases) {
            assertEquals(item.definition(), REGEX.matchEnum(item.command()), item.command());
            assertNotNull(item.definition().getMethodEnum(), item.definition().name());
            assertFalse(item.command().contains("乾坤一掷"));
            assertFalse(item.command().contains("角色名"));
            assertFalse(item.command().contains("570790267"));
        }
    }
}
