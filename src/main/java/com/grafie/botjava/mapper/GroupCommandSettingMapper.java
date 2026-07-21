package com.grafie.botjava.mapper;

import com.grafie.botjava.entity.GroupCommandSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupCommandSettingMapper extends JpaRepository<GroupCommandSetting, Long> {

    GroupCommandSetting findByGroupOpenIdAndCommandName(String groupOpenId, String commandName);

    List<GroupCommandSetting> findByGroupOpenIdOrderByCommandNameAsc(String groupOpenId);
}
