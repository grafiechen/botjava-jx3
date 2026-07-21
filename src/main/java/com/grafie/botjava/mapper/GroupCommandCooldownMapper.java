package com.grafie.botjava.mapper;

import com.grafie.botjava.entity.GroupCommandCooldown;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

public interface GroupCommandCooldownMapper extends JpaRepository<GroupCommandCooldown, Long> {

    GroupCommandCooldown findByGroupOpenIdAndCommandName(String groupOpenId, String commandName);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            update GroupCommandCooldown c
               set c.reservationId = :reservationId,
                   c.reservedAt = :now,
                   c.leaseExpiresAt = :leaseExpiresAt,
                   c.availableAt = null,
                   c.updateTime = :now
             where c.groupOpenId = :groupOpenId
               and c.commandName = :commandName
               and (c.reservationId is null or c.leaseExpiresAt <= :now)
               and (c.availableAt is null or c.availableAt <= :now)
            """)
    int reserveExisting(@Param("groupOpenId") String groupOpenId,
                        @Param("commandName") String commandName,
                        @Param("reservationId") String reservationId,
                        @Param("now") Instant now,
                        @Param("leaseExpiresAt") Instant leaseExpiresAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            update GroupCommandCooldown c
               set c.reservationId = null,
                   c.leaseExpiresAt = null,
                   c.availableAt = :availableAt,
                   c.updateTime = :completedAt
             where c.groupOpenId = :groupOpenId
               and c.commandName = :commandName
               and c.reservationId = :reservationId
            """)
    int completeReservation(@Param("groupOpenId") String groupOpenId,
                            @Param("commandName") String commandName,
                            @Param("reservationId") String reservationId,
                            @Param("completedAt") Instant completedAt,
                            @Param("availableAt") Instant availableAt);
}
