package com.grafie.botjava.service;

import com.grafie.botjava.entity.ScriptStatusFieldDefinition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MongoDisplayValueFormatterTest {
    private final MongoDisplayValueFormatter formatter = new MongoDisplayValueFormatter("Asia/Tokyo");

    @Test
    void shouldSupportRealRolesDumpTimeFormats() {
        assertThat(formatter.format(1786500166, ScriptStatusFieldDefinition.ValueType.DATETIME))
                .isEqualTo("2026-08-12 11:02:46");
        assertThat(formatter.format("2026-8-12-10-4-29", ScriptStatusFieldDefinition.ValueType.DATETIME))
                .isEqualTo("2026-08-12 10:04:29");
        assertThat(formatter.format(27655, ScriptStatusFieldDefinition.ValueType.INTEGER)).isEqualTo("27655");
    }
}