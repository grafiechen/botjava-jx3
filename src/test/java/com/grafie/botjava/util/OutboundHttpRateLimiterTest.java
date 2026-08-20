package com.grafie.botjava.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboundHttpRateLimiterTest {
    @AfterEach
    void disableAfterTest() {
        OutboundHttpRateLimiter.configure(false, 2);
    }

    @Test
    void shouldSpaceThreeRequestStartsAcrossAtLeastOneSecond() {
        OutboundHttpRateLimiter.configure(true, 2);
        long started = System.nanoTime();
        OutboundHttpRateLimiter.awaitPermit();
        OutboundHttpRateLimiter.awaitPermit();
        OutboundHttpRateLimiter.awaitPermit();
        long elapsedMillis = (System.nanoTime() - started) / 1_000_000;
        assertThat(elapsedMillis).isGreaterThanOrEqualTo(950);
    }

    @Test
    void shouldRejectRatesAboveTwoPerSecond() {
        assertThatThrownBy(() -> OutboundHttpRateLimiter.configure(true, 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1 到 2");
    }
}