package com.grafie.botjava.mapper;

import com.grafie.botjava.entity.GroupInfo;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * @author grafie.chen
 * @since 2025/1/22  16:24
 */
public interface GroupInfoMapper extends JpaRepository<GroupInfo, Long> {
    /**
     * 根据openGroupId查询
     *
     * @param openGroupId open_group_id
     * @return entity
     */
    GroupInfo findByOpenGroupId(String openGroupId);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update GroupInfo g set g.server = :server
             where g.openGroupId = :openGroupId
               and (g.server is null or g.server = '')
            """)
    int bindServerIfEmpty(@Param("openGroupId") String openGroupId,
                          @Param("server") String server);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update GroupInfo g set g.commandsEnabled = :enabled where g.openGroupId = :openGroupId")
    int updateCommandsEnabled(@Param("openGroupId") String openGroupId,
                              @Param("enabled") boolean enabled);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update GroupInfo g set g.experimentalEnabled = :enabled where g.openGroupId = :openGroupId")
    int updateExperimentalEnabled(@Param("openGroupId") String openGroupId,
                                  @Param("enabled") boolean enabled);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update GroupInfo g set g.activeMessagesEnabled = :enabled where g.openGroupId = :openGroupId")
    int updateActiveMessagesEnabled(@Param("openGroupId") String openGroupId,
                                    @Param("enabled") boolean enabled);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update GroupInfo g set g.activeMessagesPlatformAllowed = :allowed where g.openGroupId = :openGroupId")
    int updateActiveMessagesPlatformAllowed(@Param("openGroupId") String openGroupId,
                                            @Param("allowed") boolean allowed);

    /**
     * 在数据库中原子抢占当前群的主动消息发送窗口。
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update GroupInfo g
               set g.activeMessageReservedAt = :reservedAt,
                   g.activeMessageReservationId = :reservationId
             where g.openGroupId = :openGroupId
               and g.activeMessagesEnabled = true
               and g.activeMessagesPlatformAllowed = true
               and (g.activeMessageReservedAt is null or g.activeMessageReservedAt <= :availableBefore)
            """)
    int reserveActiveMessage(@Param("openGroupId") String openGroupId,
                             @Param("reservedAt") Instant reservedAt,
                             @Param("availableBefore") Instant availableBefore,
                             @Param("reservationId") String reservationId);

    /**
     * 发送失败时仅回滚本次占位，避免清除其他实例后来取得的窗口。
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update GroupInfo g
               set g.activeMessageReservedAt = null,
                   g.activeMessageReservationId = null
             where g.openGroupId = :openGroupId
               and g.activeMessageReservationId = :reservationId
            """)
    int rollbackActiveMessage(@Param("openGroupId") String openGroupId,
                              @Param("reservationId") String reservationId);
}
