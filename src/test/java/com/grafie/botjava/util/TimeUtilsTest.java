package com.grafie.botjava.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeUtilsTest {

    @Test
    void shouldFormatJx3TimestampInChinaStandardTime() {
        assertEquals("2024-12-06 21:15:00", TimeUtils.timeFormatting(1733490900L));
        assertEquals("", TimeUtils.timeFormatting(0L));
    }
}
