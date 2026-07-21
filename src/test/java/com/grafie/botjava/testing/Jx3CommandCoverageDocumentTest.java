package com.grafie.botjava.testing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.jx3.http.util.REGEX;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Jx3CommandCoverageDocumentTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Path COVERAGE_DOC = Path.of("docs", "testing", "jx3api-command-coverage.json");
    private static final Path HTTP_DOC = Path.of("docs", "testing", "jx3api-http-check.json");

    @Test
    void shouldMatchEveryRegisteredHttpCommandAndOfficialContract() throws Exception {
        Map<String, Object> coverage = OBJECT_MAPPER.readValue(COVERAGE_DOC.toFile(), new TypeReference<>() {
        });
        Map<String, Object> httpDocument = OBJECT_MAPPER.readValue(HTTP_DOC.toFile(), new TypeReference<>() {
        });
        List<Map<String, Object>> mappings = OBJECT_MAPPER.convertValue(
                coverage.get("commandMappings"), new TypeReference<>() {
                });
        List<Map<String, Object>> contracts = OBJECT_MAPPER.convertValue(
                httpDocument.get("httpContracts"), new TypeReference<>() {
                });

        assertEquals(1, ((Number) coverage.get("schemaVersion")).intValue());
        assertEquals(contracts.size(), ((Number) coverage.get("totalHttpContracts")).intValue());

        Map<String, String> documentedMappings = new HashMap<>();
        Map<String, String> coverageTypes = new HashMap<>();
        for (Map<String, Object> mapping : mappings) {
            String regex = required(mapping, "regex");
            String methodEnum = required(mapping, "methodEnum");
            String coverageType = required(mapping, "coverageType");
            assertTrue(Set.of("OFFICIAL", "LEGACY").contains(coverageType),
                    "未知覆盖类型：" + coverageType);
            assertTrue(documentedMappings.put(regex, methodEnum) == null, "重复的 REGEX 映射：" + regex);
            coverageTypes.put(regex, coverageType);
        }

        Map<String, String> actualMappings = java.util.Arrays.stream(REGEX.values())
                .filter(regex -> regex.getMethodEnum() != null)
                .collect(Collectors.toMap(REGEX::name, regex -> regex.getMethodEnum().name()));
        assertEquals(actualMappings, documentedMappings, "群指令覆盖矩阵必须与 REGEX 一致");

        Set<String> contractMethods = contracts.stream()
                .map(contract -> required(contract, "methodEnum"))
                .collect(Collectors.toCollection(HashSet::new));
        Set<String> officialCommandMethods = documentedMappings.entrySet().stream()
                .filter(entry -> "OFFICIAL".equals(coverageTypes.get(entry.getKey())))
                .map(Map.Entry::getValue)
                .collect(Collectors.toCollection(HashSet::new));
        assertEquals(contractMethods, officialCommandMethods,
                "每个官方 HTTP contract 都必须注册为群指令，官方群指令也必须具备 contract");
        for (Map.Entry<String, String> entry : documentedMappings.entrySet()) {
            if ("OFFICIAL".equals(coverageTypes.get(entry.getKey()))) {
                assertTrue(contractMethods.contains(entry.getValue()),
                        "官方群指令缺少 HTTP contract：" + entry.getValue());
            } else {
                assertTrue(!contractMethods.contains(entry.getValue()),
                        "已进入官方 contract 的接口不应继续标记为 LEGACY：" + entry.getValue());
            }
        }
    }

    private static String required(Map<String, Object> value, String key) {
        Object field = value.get(key);
        assertNotNull(field, "缺少字段：" + key);
        assertTrue(!field.toString().isBlank(), "字段为空：" + key);
        return field.toString();
    }
}
