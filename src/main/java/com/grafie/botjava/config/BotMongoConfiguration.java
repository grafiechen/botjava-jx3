package com.grafie.botjava.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "bot.mongodb", name = "enabled", havingValue = "true")
public class BotMongoConfiguration {

    @Bean(destroyMethod = "close")
    public MongoClient botMongoClient(BotMongoProperties properties) {
        return MongoClients.create(properties.getUri());
    }

    @Bean
    public MongoTemplate botMongoTemplate(MongoClient botMongoClient, BotMongoProperties properties) {
        return new MongoTemplate(botMongoClient, properties.getDatabase());
    }
}