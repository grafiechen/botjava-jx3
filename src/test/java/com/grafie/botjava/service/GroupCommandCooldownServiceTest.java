package com.grafie.botjava.service;

import com.grafie.botjava.config.CommandCooldownProperties;
import com.grafie.botjava.entity.GroupCommandCooldown;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.mapper.GroupCommandCooldownMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GroupCommandCooldownServiceTest {

    @Test
    void shouldUseFiveSecondsForInternalCommand() {
        MutableClock clock = new MutableClock();
        GroupCommandCooldownService service = service(new CommandCooldownProperties(), clock);

        GroupCommandCooldownService.Decision acquired = service.tryAcquire("group-1", REGEX.Help);
        assertTrue(acquired.allowed());
        assertEquals(300, service.tryAcquire("group-1", REGEX.Help).retryAfterSeconds());
        service.complete(acquired);
        assertEquals(5, service.tryAcquire("group-1", REGEX.Help).retryAfterSeconds());
        clock.advance(Duration.ofSeconds(5));
        assertTrue(service.tryAcquire("group-1", REGEX.Help).allowed());
    }

    @Test
    void shouldUseThirtySecondsForExternalAndExplicitDpsCommands() {
        GroupCommandCooldownService service = service(new CommandCooldownProperties(), new MutableClock());

        assertTrue(service.tryAcquire("group-1", REGEX.ServerCheck).allowed());
        assertTrue(service.tryAcquire("group-1", REGEX.DpsCompute).allowed());
    }

    @Test
    void shouldIsolateGroupsAndCommands() {
        GroupCommandCooldownService service = service(new CommandCooldownProperties(), new MutableClock());

        assertTrue(service.tryAcquire("group-1", REGEX.ServerCheck).allowed());
        assertTrue(service.tryAcquire("group-2", REGEX.ServerCheck).allowed());
        assertTrue(service.tryAcquire("group-1", REGEX.ActiveCurrent).allowed());
        assertFalse(service.tryAcquire("group-1", REGEX.ServerCheck).allowed());
    }

    @Test
    void shouldSupportPerCommandOverrideAndZeroToDisable() {
        CommandCooldownProperties properties = new CommandCooldownProperties();
        properties.setCooldownSeconds(Map.of("ServerCheck", 12, "Help", 0));
        properties.validate();
        GroupCommandCooldownService service = service(properties, new MutableClock());

        GroupCommandCooldownService.Decision acquired = service.tryAcquire("group-1", REGEX.ServerCheck);
        service.complete(acquired);
        assertEquals(12, service.tryAcquire("group-1", REGEX.ServerCheck).retryAfterSeconds());
        assertTrue(service.tryAcquire("group-1", REGEX.Help).allowed());
        assertTrue(service.tryAcquire("group-1", REGEX.Help).allowed());
    }

    @Test
    void shouldAllowOnlyOneConcurrentRequestForSameGroupAndCommand() throws Exception {
        GroupCommandCooldownService service = service(new CommandCooldownProperties(), new MutableClock());
        int requestCount = 12;
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Boolean>> futures = java.util.stream.IntStream.range(0, requestCount)
                    .mapToObj(ignored -> executor.submit(() -> {
                        start.await();
                        return service.tryAcquire("group-1", REGEX.ServerCheck).allowed();
                    }))
                    .toList();
            start.countDown();

            long allowedCount = 0;
            for (Future<Boolean> future : futures) {
                if (future.get()) {
                    allowedCount++;
                }
            }
            assertEquals(1, allowedCount);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldStartCooldownAfterSlowRequestCompletesAndRecoverExpiredLease() {
        MutableClock clock = new MutableClock();
        GroupCommandCooldownService service = service(new CommandCooldownProperties(), clock);

        GroupCommandCooldownService.Decision acquired = service.tryAcquire("group-1", REGEX.ServerCheck);
        clock.advance(Duration.ofSeconds(40));
        assertFalse(service.tryAcquire("group-1", REGEX.ServerCheck).allowed());

        service.complete(acquired);
        clock.advance(Duration.ofSeconds(29));
        assertFalse(service.tryAcquire("group-1", REGEX.ServerCheck).allowed());
        clock.advance(Duration.ofSeconds(1));
        assertTrue(service.tryAcquire("group-1", REGEX.ServerCheck).allowed());

        clock.advance(Duration.ofSeconds(300));
        assertTrue(service.tryAcquire("group-1", REGEX.ServerCheck).allowed());
    }

    private static GroupCommandCooldownService service(CommandCooldownProperties properties, Clock clock) {
        return new GroupCommandCooldownService(properties, fakeMapper(), clock);
    }

    private static GroupCommandCooldownMapper fakeMapper() {
        GroupCommandCooldownMapper mapper = mock(GroupCommandCooldownMapper.class);
        Map<String, GroupCommandCooldown> states = new HashMap<>();
        when(mapper.reserveExisting(anyString(), anyString(), anyString(), any(Instant.class), any(Instant.class)))
                .thenAnswer(invocation -> {
                    synchronized (states) {
                        String key = key(invocation.getArgument(0), invocation.getArgument(1));
                        GroupCommandCooldown state = states.get(key);
                        Instant now = invocation.getArgument(3);
                        if (state == null || isBlocked(state, now)) {
                            return 0;
                        }
                        state.setReservationId(invocation.getArgument(2));
                        state.setReservedAt(now);
                        state.setLeaseExpiresAt(invocation.getArgument(4));
                        state.setAvailableAt(null);
                        return 1;
                    }
                });
        when(mapper.saveAndFlush(any(GroupCommandCooldown.class))).thenAnswer(invocation -> {
            synchronized (states) {
                GroupCommandCooldown state = invocation.getArgument(0);
                String key = key(state.getGroupOpenId(), state.getCommandName());
                if (states.containsKey(key)) {
                    throw new DataIntegrityViolationException("duplicate cooldown key");
                }
                states.put(key, state);
                return state;
            }
        });
        when(mapper.findByGroupOpenIdAndCommandName(anyString(), anyString())).thenAnswer(invocation -> {
            synchronized (states) {
                return states.get(key(invocation.getArgument(0), invocation.getArgument(1)));
            }
        });
        when(mapper.completeReservation(anyString(), anyString(), anyString(),
                any(Instant.class), any(Instant.class))).thenAnswer(invocation -> {
            synchronized (states) {
                GroupCommandCooldown state = states.get(key(invocation.getArgument(0), invocation.getArgument(1)));
                if (state == null || !invocation.getArgument(2).equals(state.getReservationId())) {
                    return 0;
                }
                state.setReservationId(null);
                state.setLeaseExpiresAt(null);
                state.setAvailableAt(invocation.getArgument(4));
                return 1;
            }
        });
        return mapper;
    }

    private static boolean isBlocked(GroupCommandCooldown state, Instant now) {
        boolean inFlight = state.getReservationId() != null
                && state.getLeaseExpiresAt() != null && state.getLeaseExpiresAt().isAfter(now);
        boolean cooling = state.getAvailableAt() != null && state.getAvailableAt().isAfter(now);
        return inFlight || cooling;
    }

    private static String key(String groupOpenId, String commandName) {
        return groupOpenId + "\n" + commandName;
    }

    private static class MutableClock extends Clock {
        private Instant instant = Instant.parse("2026-07-11T00:00:00Z");

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
