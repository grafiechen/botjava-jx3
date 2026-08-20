package com.grafie.botjava.mapper;

import com.grafie.botjava.entity.BotRuntimeConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BotRuntimeConfigMapper extends JpaRepository<BotRuntimeConfig, Long> {

    Optional<BotRuntimeConfig> findByConfigKey(String configKey);

    List<BotRuntimeConfig> findAllByOrderByConfigKeyAsc();
}