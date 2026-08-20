package com.grafie.botjava.mapper;

import com.grafie.botjava.entity.GroupRequirement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupRequirementMapper extends JpaRepository<GroupRequirement, Long> {

    GroupRequirement findByGroupOpenIdAndTitle(String groupOpenId, String title);

    List<GroupRequirement> findByGroupOpenIdOrderByCreateTimeAscIdAsc(String groupOpenId);
}
