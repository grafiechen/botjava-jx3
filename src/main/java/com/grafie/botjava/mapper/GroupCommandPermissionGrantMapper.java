package com.grafie.botjava.mapper;

import com.grafie.botjava.entity.GroupCommandPermissionGrant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupCommandPermissionGrantMapper extends JpaRepository<GroupCommandPermissionGrant, Long> {
    boolean existsByGroupOpenIdAndMemberOpenIdAndPermissionKeyAndEnabledTrue(
            String groupOpenId, String memberOpenId, String permissionKey);
}