package com.grafie.botjava.persistence;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PostgresLiveSmokePlanTest {

    @Test
    void shouldRequireConfirmationCredentialsAndPostgresUrl() {
        Map<String, String> missingConfirmation = validEnvironment();
        missingConfirmation.remove("POSTGRES_LIVE_CONFIRM");
        assertThrows(IllegalArgumentException.class,
                () -> PostgresLiveSmokePlan.from(missingConfirmation));

        Map<String, String> missingPassword = validEnvironment();
        missingPassword.remove("POSTGRES_LIVE_PASSWORD");
        assertThrows(IllegalArgumentException.class,
                () -> PostgresLiveSmokePlan.from(missingPassword));

        Map<String, String> h2 = validEnvironment();
        h2.put("POSTGRES_LIVE_JDBC_URL", "jdbc:h2:mem:test");
        assertThrows(IllegalArgumentException.class,
                () -> PostgresLiveSmokePlan.from(h2));
    }

    @Test
    void shouldOnlyAllowMatchingDedicatedSmokeSchema() {
        Map<String, String> productionSchema = validEnvironment();
        productionSchema.put("POSTGRES_LIVE_SCHEMA", "public");
        productionSchema.put("POSTGRES_LIVE_JDBC_URL",
                "jdbc:postgresql://db.example.com:5432/bot?currentSchema=public");
        assertThrows(IllegalArgumentException.class,
                () -> PostgresLiveSmokePlan.from(productionSchema));

        Map<String, String> mismatch = validEnvironment();
        mismatch.put("POSTGRES_LIVE_SCHEMA", "botjava_smoke_other");
        assertThrows(IllegalArgumentException.class,
                () -> PostgresLiveSmokePlan.from(mismatch));

        PostgresLiveSmokePlan.Plan plan = PostgresLiveSmokePlan.from(validEnvironment());
        assertEquals("botjava_smoke_acceptance", plan.schema());
    }

    private static Map<String, String> validEnvironment() {
        Map<String, String> values = new HashMap<>();
        values.put("POSTGRES_LIVE_CONFIRM", PostgresLiveSmokePlan.CONFIRMATION);
        values.put("POSTGRES_LIVE_JDBC_URL",
                "jdbc:postgresql://db.example.com:5432/bot?sslmode=require&currentSchema=botjava_smoke_acceptance");
        values.put("POSTGRES_LIVE_USERNAME", "smoke_user");
        values.put("POSTGRES_LIVE_PASSWORD", "secret");
        values.put("POSTGRES_LIVE_SCHEMA", "botjava_smoke_acceptance");
        return values;
    }
}
