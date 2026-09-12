package com.grafie.botjava.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grafie.botjava.config.ActiveMessageProperties;
import com.grafie.botjava.config.CommandCooldownProperties;
import com.grafie.botjava.entity.CommandInvocation;
import com.grafie.botjava.entity.CommandInvocationStatus;
import com.grafie.botjava.entity.dto.common.AuthorDto;
import com.grafie.botjava.entity.dto.group.at.GroupAtMessageCreateDto;
import com.grafie.botjava.jx3.http.RequestResult;
import com.grafie.botjava.jx3.http.cache.Jx3ApiResponseCache;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.mapper.CommandInvocationMapper;
import com.grafie.botjava.mapper.GroupCommandCooldownMapper;
import com.grafie.botjava.mapper.GroupInfoMapper;
import com.grafie.botjava.mapper.Jx3ApiCacheEntryMapper;
import com.grafie.botjava.mapper.UserInfoMapper;
import com.grafie.botjava.mapper.UserRoleBindingMapper;
import com.grafie.botjava.service.GroupActiveMessagePolicy;
import com.grafie.botjava.service.GroupCommandCooldownService;
import com.grafie.botjava.service.GroupConfigurationService;
import com.grafie.botjava.service.UserCommandPreferenceService;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Starts two independent Spring/JPA contexts against one dedicated PostgreSQL
 * schema and verifies shared state plus atomic reservations.
 */
class PostgresTwoInstanceLiveSmokeIT {

    @Test
    void shouldShareBindingsCooldownActiveWindowAndCacheAcrossTwoContexts() throws Exception {
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("POSTGRES_LIVE_SMOKE")),
                "Set POSTGRES_LIVE_SMOKE=true to use the dedicated PostgreSQL smoke schema");
        PostgresLiveSmokePlan.Plan plan = PostgresLiveSmokePlan.from(System.getenv());
        String password = requiredEnvironment("POSTGRES_LIVE_PASSWORD");

        ConfigurableApplicationContext first = null;
        ConfigurableApplicationContext second = null;
        boolean cleanupAllowed = false;
        try {
            first = openContext("postgres-smoke-first", plan, password);
            second = openContext("postgres-smoke-second", plan, password);
            requireEmptySchema(first);
            cleanupAllowed = true;

            String runId = UUID.randomUUID().toString().replace("-", "");
            verifyGroupBinding(first, second, "smoke-group-" + runId);
            verifyUserBinding(first, second, "smoke-member-" + runId);
            verifyCooldownCompetition(first, second, "smoke-cooldown-" + runId);
            verifyExpiredCooldownLeaseRecovery(first, second, "smoke-lease-" + runId);
            verifyActiveMessageCompetition(first, second, "smoke-active-" + runId);
            verifySharedCache(first, second, "smoke-server-" + runId);
            verifySharedInvocationAudit(first, second, "smoke" + runId.substring(0, 8));
            System.out.println("POSTGRES_TWO_INSTANCE\tSHARED_STATE_AND_RESERVATIONS\tSUCCESS");
        }
        finally {
            try {
                clearSmokeSchemaIfAllowed(cleanupAllowed, first);
            }
            finally {
                close(second);
                close(first);
            }
        }
    }

    private static void verifyGroupBinding(ConfigurableApplicationContext first,
                                           ConfigurableApplicationContext second,
                                           String groupKey) {
        first.getBean(GroupConfigurationService.class).bindServer(groupKey, "测试服务器");
        assertEquals(Optional.of("测试服务器"),
                second.getBean(GroupConfigurationService.class).findServer(groupKey));
    }

    private static void verifyUserBinding(ConfigurableApplicationContext first,
                                          ConfigurableApplicationContext second,
                                          String memberKey) {
        GroupAtMessageCreateDto message = new GroupAtMessageCreateDto();
        AuthorDto author = new AuthorDto();
        author.setMemberOpenid(memberKey);
        message.setAuthor(author);
        message.setGroupOpenid("smoke-group-" + memberKey);
        first.getBean(UserCommandPreferenceService.class)
                .bind(message, "测试服务器", "测试角色");
        UserCommandPreferenceService.BindingSnapshot snapshot = second
                .getBean(UserCommandPreferenceService.class).findBindings(message);
        assertEquals("测试服务器", snapshot.defaultRole().getServer());
        assertEquals("测试角色", snapshot.defaultRole().getRoleName());
        assertEquals(null, snapshot.defaultRole().getSchool());
        assertEquals(1, snapshot.roles().size());
    }

    private static void verifyCooldownCompetition(ConfigurableApplicationContext first,
                                                  ConfigurableApplicationContext second,
                                                  String groupKey) throws Exception {
        GroupCommandCooldownService firstService = first.getBean(GroupCommandCooldownService.class);
        GroupCommandCooldownService secondService = second.getBean(GroupCommandCooldownService.class);
        List<GroupCommandCooldownService.Decision> decisions = concurrently(
                () -> firstService.tryAcquire(groupKey, REGEX.ServerCheck),
                () -> secondService.tryAcquire(groupKey, REGEX.ServerCheck));
        assertEquals(1, decisions.stream().filter(GroupCommandCooldownService.Decision::allowed).count());
        assertEquals(1, decisions.stream().filter(decision -> !decision.allowed()).count());
        decisions.stream().filter(GroupCommandCooldownService.Decision::allowed)
                .findFirst().ifPresent(firstService::complete);
        assertFalse(secondService.tryAcquire(groupKey, REGEX.ServerCheck).allowed());
    }

    private static void verifyActiveMessageCompetition(ConfigurableApplicationContext first,
                                                       ConfigurableApplicationContext second,
                                                       String groupKey) throws Exception {
        GroupConfigurationService configuration = first.getBean(GroupConfigurationService.class);
        configuration.setEnabled(groupKey, GroupConfigurationService.Setting.ACTIVE_MESSAGES, true);
        configuration.setActiveMessagesPlatformAllowed(groupKey, true);
        GroupActiveMessagePolicy firstPolicy = first.getBean(GroupActiveMessagePolicy.class);
        GroupActiveMessagePolicy secondPolicy = second.getBean(GroupActiveMessagePolicy.class);
        List<GroupActiveMessagePolicy.Permit> permits = concurrently(
                () -> firstPolicy.acquire(groupKey),
                () -> secondPolicy.acquire(groupKey));
        assertEquals(1, permits.stream().filter(GroupActiveMessagePolicy.Permit::allowed).count());
        GroupActiveMessagePolicy.Permit winner = permits.stream()
                .filter(GroupActiveMessagePolicy.Permit::allowed).findFirst().orElseThrow();
        firstPolicy.rollback(winner);
        assertTrue(secondPolicy.acquire(groupKey).allowed());
    }

    private static void verifyExpiredCooldownLeaseRecovery(
            ConfigurableApplicationContext first,
            ConfigurableApplicationContext second,
            String groupKey) {
        GroupCommandCooldownService.Decision abandoned = first
                .getBean(GroupCommandCooldownService.class)
                .tryAcquire(groupKey, REGEX.ServerCheck);
        assertTrue(abandoned.allowed());

        GroupCommandCooldownMapper mapper = first.getBean(GroupCommandCooldownMapper.class);
        var state = mapper.findByGroupOpenIdAndCommandName(groupKey, REGEX.ServerCheck.name());
        state.setLeaseExpiresAt(Instant.now().minusSeconds(1));
        mapper.saveAndFlush(state);

        GroupCommandCooldownService.Decision recovered = second
                .getBean(GroupCommandCooldownService.class)
                .tryAcquire(groupKey, REGEX.ServerCheck);
        assertTrue(recovered.allowed());
        second.getBean(GroupCommandCooldownService.class).complete(recovered);
    }

    private static void verifySharedCache(ConfigurableApplicationContext first,
                                          ConfigurableApplicationContext second,
                                          String server) {
        Jx3ApiResponseCache firstCache = first.getBean(Jx3ApiResponseCache.class);
        Jx3ApiResponseCache secondCache = second.getBean(Jx3ApiResponseCache.class);
        Map<String, Object> params = Map.of("server", server, "token", "not-part-of-key");
        RequestResult result = new RequestResult();
        result.setCode(200);
        result.setMsg("smoke-cache");
        result.setData(Map.of("server", server, "status", "开服"));
        firstCache.put("/data/status/check", params, result);
        assertEquals("smoke-cache", secondCache.get(
                "/data/status/check", Map.of("server", server, "token", "other-token"))
                .orElseThrow().getMsg());
    }

    private static void verifySharedInvocationAudit(ConfigurableApplicationContext first,
                                                    ConfigurableApplicationContext second,
                                                    String invocationId) {
        CommandInvocation invocation = new CommandInvocation();
        invocation.setInvocationId(invocationId);
        invocation.setGroupOpenId("smoke-audit-group");
        invocation.setMemberOpenId("smoke-audit-member");
        invocation.setCommandName(REGEX.ServerCheck.name());
        invocation.setCommandGroup(REGEX.ServerCheck.getCommandGroup().name());
        invocation.setExternalCall(true);
        invocation.setStatus(CommandInvocationStatus.SUCCESS);
        invocation.setResponseType("TEXT");
        invocation.setElapsedMillis(1);
        first.getBean(CommandInvocationMapper.class).saveAndFlush(invocation);

        assertEquals(1, second.getBean(CommandInvocationMapper.class).findAll().stream()
                .filter(item -> invocationId.equals(item.getInvocationId()))
                .count());
    }

    private static <T> List<T> concurrently(CheckedSupplier<T> first,
                                            CheckedSupplier<T> second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<T> firstFuture = executor.submit(() -> awaitAndGet(ready, start, first));
            Future<T> secondFuture = executor.submit(() -> awaitAndGet(ready, start, second));
            ready.await();
            start.countDown();
            return List.of(firstFuture.get(), secondFuture.get());
        }
        finally {
            executor.shutdownNow();
        }
    }

    private static <T> T awaitAndGet(CountDownLatch ready, CountDownLatch start,
                                     CheckedSupplier<T> supplier) throws Exception {
        ready.countDown();
        start.await();
        return supplier.get();
    }

    private static ConfigurableApplicationContext openContext(
            String applicationName, PostgresLiveSmokePlan.Plan plan, String password) {
        SpringApplication application = new SpringApplication(PersistenceSmokeConfiguration.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(
                "postgres-live-smoke", Map.ofEntries(
                Map.entry("spring.application.name", applicationName),
                Map.entry("spring.datasource.url", plan.jdbcUrl()),
                Map.entry("spring.datasource.username", plan.username()),
                Map.entry("spring.datasource.password", password),
                Map.entry("spring.datasource.driver-class-name", "org.postgresql.Driver"),
                Map.entry("spring.datasource.hikari.maximum-pool-size", "2"),
                Map.entry("spring.jpa.hibernate.ddl-auto", "update"),
                Map.entry("spring.jpa.open-in-view", "false"),
                Map.entry("spring.main.banner-mode", "off"),
                Map.entry("spring.task.scheduling.enabled", "false"),
                Map.entry("logging.level.root", "WARN")
        )));
        application.setEnvironment(environment);
        return application.run();
    }

    private static void requireEmptySchema(ConfigurableApplicationContext context) {
        List<String> nonEmpty = new ArrayList<>();
        context.getBeansOfType(JpaRepository.class).forEach((name, repository) -> {
            if (repository.count() != 0) {
                nonEmpty.add(name);
            }
        });
        if (!nonEmpty.isEmpty()) {
            throw new IllegalStateException(
                    "Dedicated smoke schema must contain no business rows: " + nonEmpty);
        }
    }

    static void clearSmokeSchemaIfAllowed(boolean cleanupAllowed,
                                          ConfigurableApplicationContext context) {
        if (!cleanupAllowed) {
            return;
        }
        context.getBeansOfType(JpaRepository.class).forEach((name, repository) ->
                repository.deleteAllInBatch());
    }

    private static void close(ConfigurableApplicationContext context) {
        if (context != null) {
            context.close();
        }
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }
        return value.trim();
    }

    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get() throws Exception;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableTransactionManagement
    @EntityScan(basePackages = "com.grafie.botjava.entity")
    @EnableJpaRepositories(basePackages = "com.grafie.botjava.mapper")
    static class PersistenceSmokeConfiguration {

        @Bean
        CommandCooldownProperties commandCooldownProperties() {
            return new CommandCooldownProperties();
        }

        @Bean
        ActiveMessageProperties activeMessageProperties() {
            return new ActiveMessageProperties();
        }

        @Bean
        GroupCommandCooldownService cooldownService(CommandCooldownProperties properties,
                                                    GroupCommandCooldownMapper mapper) {
            return new GroupCommandCooldownService(properties, mapper);
        }

        @Bean
        GroupConfigurationService groupConfigurationService(GroupInfoMapper mapper) {
            return new GroupConfigurationService(mapper);
        }

        @Bean
        GroupActiveMessagePolicy activeMessagePolicy(GroupInfoMapper mapper,
                                                     ActiveMessageProperties properties) {
            return new GroupActiveMessagePolicy(mapper, properties);
        }

        @Bean
        UserCommandPreferenceService userPreferenceService(UserInfoMapper userInfoMapper,
                                                           UserRoleBindingMapper roleMapper) {
            return new UserCommandPreferenceService(userInfoMapper, roleMapper);
        }

        @Bean
        Jx3ApiResponseCache responseCache(Jx3ApiCacheEntryMapper mapper,
                                          ObjectMapper objectMapper) {
            return new Jx3ApiResponseCache(mapper, objectMapper);
        }
    }
}
