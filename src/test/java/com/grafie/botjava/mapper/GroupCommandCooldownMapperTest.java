package com.grafie.botjava.mapper;

import com.grafie.botjava.config.CommandCooldownProperties;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.service.GroupCommandCooldownService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "tx.bot.openapi-url=https://example.test",
        "tx.bot.access-token-url=https://example.test/token",
        "tx.bot.app-id=test-app",
        "tx.bot.app-secret=test-secret",
        "jx3api.api.api-token=test-token",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class GroupCommandCooldownMapperTest {

    @Autowired
    private GroupCommandCooldownMapper cooldownMapper;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldShareCooldownAcrossInstancesAndProtectNewReservationFromOldCompletion() {
        MutableClock clock = new MutableClock();
        CommandCooldownProperties properties = new CommandCooldownProperties();
        GroupCommandCooldownService first = new GroupCommandCooldownService(properties, cooldownMapper, clock);
        GroupCommandCooldownService second = new GroupCommandCooldownService(properties, cooldownMapper, clock);

        GroupCommandCooldownService.Decision firstPermit = first.tryAcquire("shared-group", REGEX.ServerCheck);
        assertTrue(firstPermit.allowed());
        assertFalse(second.tryAcquire("shared-group", REGEX.ServerCheck).allowed());

        first.complete(firstPermit);
        assertEquals(30, second.tryAcquire("shared-group", REGEX.ServerCheck).retryAfterSeconds());
        clock.advance(Duration.ofSeconds(30));
        GroupCommandCooldownService.Decision secondPermit = second.tryAcquire("shared-group", REGEX.ServerCheck);
        assertTrue(secondPermit.allowed());

        first.complete(firstPermit);
        assertFalse(first.tryAcquire("shared-group", REGEX.ServerCheck).allowed());

        clock.advance(Duration.ofSeconds(300));
        assertTrue(first.tryAcquire("shared-group", REGEX.ServerCheck).allowed());
    }

    private static class MutableClock extends Clock {
        private Instant instant = Instant.parse("2026-07-13T08:00:00Z");

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
