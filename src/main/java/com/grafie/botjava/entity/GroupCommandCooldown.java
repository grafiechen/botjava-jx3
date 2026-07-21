package com.grafie.botjava.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * 群指令跨实例共享的冷却与执行占位。
 */
@Data
@Entity
@Table(name = "group_command_cooldown", uniqueConstraints = @UniqueConstraint(
        name = "uk_group_command_cooldown_group_command",
        columnNames = {"group_open_id", "command_name"}
))
public class GroupCommandCooldown {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_open_id", nullable = false, length = 128)
    private String groupOpenId;

    @Column(name = "command_name", nullable = false, length = 64)
    private String commandName;

    @Column(name = "reservation_id", length = 36)
    private String reservationId;

    @Column(name = "reserved_at")
    private Instant reservedAt;

    @Column(name = "lease_expires_at")
    private Instant leaseExpiresAt;

    @Column(name = "available_at")
    private Instant availableAt;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private Instant createTime;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private Instant updateTime;
}
