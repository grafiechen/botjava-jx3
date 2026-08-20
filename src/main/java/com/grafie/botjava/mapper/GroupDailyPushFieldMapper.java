package com.grafie.botjava.mapper;

import com.grafie.botjava.entity.GroupDailyPushField;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupDailyPushFieldMapper extends JpaRepository<GroupDailyPushField, Long> {
    List<GroupDailyPushField> findByGroupOpenIdAndEnabledTrueOrderBySortOrderAscIdAsc(String groupOpenId);
    List<GroupDailyPushField> findByGroupOpenIdOrderBySortOrderAscIdAsc(String groupOpenId);
    GroupDailyPushField findByGroupOpenIdAndMongoFieldName(String groupOpenId, String mongoFieldName);
    boolean existsByGroupOpenId(String groupOpenId);
}