package com.grafie.botjava.service;

import com.grafie.botjava.entity.BotRuntimeConfig;
import com.grafie.botjava.mapper.BotRuntimeConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class BotRuntimeConfigService {

    private static final Pattern CONFIG_KEY_PATTERN = Pattern.compile("[A-Za-z0-9._-]{1,80}");
    private static final int MAX_VALUE_LENGTH = 2048;

    private final BotRuntimeConfigMapper configMapper;

    public BotRuntimeConfigService(BotRuntimeConfigMapper configMapper) {
        this.configMapper = configMapper;
    }

    public List<BotRuntimeConfig> list() {
        return configMapper.findAllByOrderByConfigKeyAsc();
    }

    public Optional<BotRuntimeConfig> find(String key) {
        return configMapper.findByConfigKey(normalizeKey(key));
    }

    @Transactional
    public BotRuntimeConfig set(String key, String value, String updatedBy) {
        String normalizedKey = normalizeKey(key);
        String safeValue = normalizeValue(value);
        BotRuntimeConfig config = configMapper.findByConfigKey(normalizedKey).orElseGet(BotRuntimeConfig::new);
        config.setConfigKey(normalizedKey);
        config.setConfigValue(safeValue);
        config.setUpdatedBy(updatedBy);
        return configMapper.save(config);
    }

    @Transactional
    public boolean delete(String key) {
        String normalizedKey = normalizeKey(key);
        Optional<BotRuntimeConfig> existing = configMapper.findByConfigKey(normalizedKey);
        existing.ifPresent(configMapper::delete);
        return existing.isPresent();
    }

    private String normalizeKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("配置 key 不能为空");
        }
        String trimmed = key.trim();
        if (!CONFIG_KEY_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("配置 key 只能包含字母、数字、点、下划线和中划线，长度 1 到 80");
        }
        return trimmed;
    }

    private String normalizeValue(String value) {
        String safeValue = value == null ? "" : value.trim();
        if (safeValue.length() > MAX_VALUE_LENGTH) {
            throw new IllegalArgumentException("配置值不能超过 2048 个字符");
        }
        return safeValue;
    }
}