package com.grafie.botjava.jx3.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.jx3.http.util.REGEX;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class Jx3ApiLiveSmokePlan {

    private static final Path COVERAGE_DOCUMENT = Path.of(
            "docs", "testing", "jx3api-command-coverage.json");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private Jx3ApiLiveSmokePlan() {
    }

    static List<SmokeCase> build(String server, String roleName, String uid) throws Exception {
        Map<String, Object> document = OBJECT_MAPPER.readValue(
                COVERAGE_DOCUMENT.toFile(), new TypeReference<>() {
                });
        List<Map<String, Object>> mappings = OBJECT_MAPPER.convertValue(
                document.get("commandMappings"), new TypeReference<>() {
                });
        Set<String> officialDefinitions = new LinkedHashSet<>();
        for (Map<String, Object> mapping : mappings) {
            if ("OFFICIAL".equals(mapping.get("coverageType"))) {
                officialDefinitions.add(mapping.get("regex").toString());
            }
        }

        return Arrays.stream(REGEX.values())
                .filter(definition -> officialDefinitions.contains(definition.name()))
                .filter(definition -> definition != REGEX.SoundConverter)
                .map(definition -> new SmokeCase(
                        definition,
                        replaceExampleValues(definition.getExample(), server, roleName, uid)))
                .toList();
    }

    static int officialContractCount() throws Exception {
        Map<String, Object> document = OBJECT_MAPPER.readValue(
                COVERAGE_DOCUMENT.toFile(), new TypeReference<>() {
                });
        return ((Number) document.get("totalHttpContracts")).intValue();
    }

    private static String replaceExampleValues(String example, String server, String roleName, String uid) {
        return example
                .replace("乾坤一掷", server)
                .replace("角色名", roleName)
                .replace("570790267", uid);
    }

    record SmokeCase(REGEX definition, String command) {
    }
}
