package com.grafie.botjava.service;

import com.grafie.botjava.entity.GroupCommandSetting;
import com.grafie.botjava.jx3.http.util.REGEX;
import com.grafie.botjava.mapper.GroupCommandSettingMapper;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 管理群维度的单条指令启停覆盖值。
 */
@Service
public class GroupCommandSettingService {

    private final GroupCommandSettingMapper mapper;

    public GroupCommandSettingService(GroupCommandSettingMapper mapper) {
        this.mapper = mapper;
    }

    public Map<REGEX, Boolean> findOverrides(String groupOpenId) {
        if (groupOpenId == null || groupOpenId.isBlank()) {
            return Map.of();
        }
        List<GroupCommandSetting> settings = mapper.findByGroupOpenIdOrderByCommandNameAsc(groupOpenId);
        if (settings == null) {
            settings = Collections.emptyList();
        }
        Map<REGEX, Boolean> result = new EnumMap<>(REGEX.class);
        for (GroupCommandSetting setting : settings) {
            REGEX definition = REGEX.findByName(setting.getCommandName());
            if (definition != null && setting.getEnabled() != null) {
                result.put(definition, setting.getEnabled());
            }
        }
        return Map.copyOf(result);
    }

    @Transactional
    public GroupCommandSetting setEnabled(String groupOpenId, REGEX definition, boolean enabled) {
        if (groupOpenId == null || groupOpenId.isBlank()) {
            throw new IllegalArgumentException("当前消息缺少群标识，无法保存群指令设置。");
        }
        if (definition == null || definition.getCommandAvailability() == REGEX.CommandAvailability.SYSTEM) {
            throw new IllegalArgumentException("系统指令不能通过群单项设置关闭。");
        }
        GroupCommandSetting setting = mapper.findByGroupOpenIdAndCommandName(groupOpenId, definition.name());
        if (setting == null) {
            setting = new GroupCommandSetting();
            setting.setGroupOpenId(groupOpenId);
            setting.setCommandName(definition.name());
        }
        setting.setEnabled(enabled);
        return mapper.save(setting);
    }
}
