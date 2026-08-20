package com.grafie.botjava.mapper;

import com.grafie.botjava.entity.GroupRequirementItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupRequirementItemMapper extends JpaRepository<GroupRequirementItem, Long> {

    List<GroupRequirementItem> findByRequirement_IdOrderByCreateTimeAscIdAsc(Long requirementId);

    long countByRequirement_Id(Long requirementId);

    long deleteByRequirement_IdAndMemberOpenId(Long requirementId, String memberOpenId);

    long deleteByRequirement_Id(Long requirementId);
}
