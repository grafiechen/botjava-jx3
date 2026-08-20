package com.grafie.botjava.mapper;

import com.grafie.botjava.entity.GroupPushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface GroupPushSubscriptionMapper extends JpaRepository<GroupPushSubscription, Long> {

    GroupPushSubscription findByGroupOpenIdAndTaskCode(String groupOpenId, String taskCode);

    List<GroupPushSubscription> findByGroupOpenIdOrderByTaskCodeAsc(String groupOpenId);

    @Query("select s.groupOpenId from GroupPushSubscription s "
            + "where s.taskCode = :taskCode and s.enabled = true order by s.groupOpenId")
    List<String> findEnabledGroupOpenIds(@Param("taskCode") String taskCode);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update GroupPushSubscription s set s.enabled = :enabled, s.updatedBy = :updatedBy "
            + "where s.groupOpenId = :groupOpenId and s.taskCode = :taskCode")
    int updateEnabled(@Param("groupOpenId") String groupOpenId,
                      @Param("taskCode") String taskCode,
                      @Param("enabled") boolean enabled,
                      @Param("updatedBy") String updatedBy);
}
