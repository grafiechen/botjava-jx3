package com.grafie.botjava.testing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Jx3ApiHttpCheckDocumentTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Path CHECK_DOC = Path.of("docs", "testing", "jx3api-http-check.json");

    @Test
    @DisplayName("CHECK_DOCUMENT_SCHEMA_IS_MACHINE_READABLE")
    void shouldKeepCheckDocumentMachineReadable() throws Exception {
        Map<String, Object> document = readDocument();

        assertEquals(1, ((Number) document.get("schemaVersion")).intValue());
        assertNotNull(document.get("purpose"));
        assertNotNull(document.get("sourceDocs"));
        assertNotNull(document.get("testFiles"));
        assertNotNull(document.get("metaTestFile"));
        List<Map<String, Object>> cases = cases(document);
        assertFalse(cases.isEmpty());
        List<Map<String, Object>> httpContracts = httpContracts(document);
        assertFalse(httpContracts.isEmpty());

        Set<String> caseIds = new HashSet<>();
        for (Map<String, Object> testCase : cases) {
            String caseId = requireString(testCase, "caseId");
            assertTrue(caseIds.add(caseId), "Duplicate caseId: " + caseId);
            requireString(testCase, "type");
            requireString(testCase, "description");
            assertTrue(testCase.containsKey("expected"), "Missing expected for " + caseId);
        }
        for (Map<String, Object> contract : httpContracts) {
            String caseId = requireString(contract, "caseId");
            assertTrue(caseIds.add(caseId), "Duplicate caseId: " + caseId);
            requireString(contract, "name");
            requireString(contract, "doc");
            requireString(contract, "methodEnum");
            requireString(contract, "expectedPath");
            requireString(contract, "dataShape");
            assertTrue(contract.containsKey("result"), "Missing result for " + caseId);
        }
    }

    @Test
    @DisplayName("CHECK_DOCUMENT_CASES_ARE_REFERENCED_BY_UNIT_TESTS")
    void shouldEnsureEveryCheckCaseIsCoveredByDeclaredUnitTests() throws Exception {
        Map<String, Object> document = readDocument();
        List<String> testFiles = OBJECT_MAPPER.convertValue(document.get("testFiles"), new TypeReference<>() {
        });
        String allTestSource = readDeclaredTestSources(testFiles);

        for (Map<String, Object> testCase : cases(document)) {
            String caseId = requireString(testCase, "caseId");
            assertTrue(allTestSource.contains(caseId), "No unit test references caseId: " + caseId);
        }
        assertTrue(allTestSource.contains("httpContracts"), "No unit test reads httpContracts");
        assertTrue(allTestSource.contains("shouldDeserializeEveryOfficialHttpResult"), "No parameterized full deserialization test found");
    }

    @Test
    @DisplayName("CHECK_DOCUMENT_HTTP_CONTRACTS_MATCH_INVENTORY")
    void shouldEnsureEveryInventoryHttpInterfaceHasContract() throws Exception {
        Map<String, Object> document = readDocument();
        List<Map<String, Object>> httpContracts = httpContracts(document);
        long inventoryCount = Files.readAllLines(Path.of("docs", "JX3API_HTTP_API_INVENTORY.md")).stream()
                .filter(line -> line.startsWith("| ") && line.contains("`doc/"))
                .count();

        assertEquals(inventoryCount, httpContracts.size(), "HTTP contract count must match inventory count");
    }

    @Test
    @DisplayName("CHECK_DOCUMENT_DECLARED_TEST_FILES_EXIST")
    void shouldEnsureDeclaredTestFilesExist() throws Exception {
        Map<String, Object> document = readDocument();
        List<String> testFiles = OBJECT_MAPPER.convertValue(document.get("testFiles"), new TypeReference<>() {
        });
        testFiles.add(requireString(document, "metaTestFile"));

        for (String testFile : testFiles) {
            assertTrue(Files.exists(Path.of(testFile)), "Declared test file does not exist: " + testFile);
        }
    }

    private static String readDeclaredTestSources(List<String> testFiles) throws Exception {
        StringBuilder source = new StringBuilder();
        for (String testFile : testFiles) {
            Path path = Path.of(testFile);
            assertTrue(Files.exists(path), "Declared test file does not exist: " + testFile);
            source.append(Files.readString(path)).append('\n');
        }
        return source.toString();
    }

    private static Map<String, Object> readDocument() throws Exception {
        assertTrue(Files.exists(CHECK_DOC), "Missing check document");
        return OBJECT_MAPPER.readValue(CHECK_DOC.toFile(), new TypeReference<>() {
        });
    }

    private static List<Map<String, Object>> cases(Map<String, Object> document) {
        return OBJECT_MAPPER.convertValue(document.get("cases"), new TypeReference<>() {
        });
    }

    private static List<Map<String, Object>> httpContracts(Map<String, Object> document) {
        return OBJECT_MAPPER.convertValue(document.get("httpContracts"), new TypeReference<>() {
        });
    }

    private static String requireString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        assertNotNull(value, "Missing key: " + key);
        assertFalse(value.toString().isBlank(), "Blank key: " + key);
        return value.toString();
    }
}
