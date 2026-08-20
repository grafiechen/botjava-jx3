package com.grafie.botjava.config;

import com.mongodb.client.MongoClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class BotMongoConfigurationTest {

    @TestConfiguration(proxyBeanMethods = false)
    @EnableConfigurationProperties(BotMongoProperties.class)
    static class MongoPropertiesTestConfiguration {
    }

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(MongoPropertiesTestConfiguration.class, BotMongoConfiguration.class);

    @Test
    void shouldNotCreateMongoBeansWhenDisabled() {
        contextRunner
                .withPropertyValues("bot.mongodb.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(MongoClient.class);
                    assertThat(context).doesNotHaveBean(MongoTemplate.class);
                });
    }

    @Test
    void shouldCreateIndependentMongoBeansWhenEnabled() {
        contextRunner
                .withPropertyValues(
                        "bot.mongodb.enabled=true",
                        "bot.mongodb.uri=mongodb://localhost:27017",
                        "bot.mongodb.database=botjava_test")
                .run(context -> {
                    assertThat(context).hasSingleBean(MongoClient.class);
                    assertThat(context).hasSingleBean(MongoTemplate.class);
                    assertThat(context.getBean(MongoTemplate.class).getDb().getName())
                            .isEqualTo("botjava_test");
                });
    }

    @Test
    void shouldRejectEnabledMongoWithoutUri() {
        contextRunner
                .withPropertyValues("bot.mongodb.enabled=true")
                .run(context -> assertThat(context).hasFailed());
    }
}