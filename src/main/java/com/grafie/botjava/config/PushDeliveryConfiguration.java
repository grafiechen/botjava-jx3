package com.grafie.botjava.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class PushDeliveryConfiguration {

    @Bean(name = "groupPushExecutor")
    public ThreadPoolTaskExecutor groupPushExecutor(
            @Value("${bot.push.delivery.core-pool-size:2}") int corePoolSize,
            @Value("${bot.push.delivery.max-pool-size:4}") int maxPoolSize,
            @Value("${bot.push.delivery.queue-capacity:200}") int queueCapacity) {
        if (corePoolSize < 1 || maxPoolSize < corePoolSize || queueCapacity < 1) {
            throw new IllegalStateException("bot.push.delivery 线程池配置不合法");
        }
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("group-push-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        return executor;
    }
}
