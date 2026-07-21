package com.grafie.botjava.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 群对单条机器人指令的启停覆盖配置。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "group_command_setting", uniqueConstraints = @UniqueConstraint(
        name = "uk_group_command_setting_group_command",
        columnNames = {"group_open_id", "command_name"}
))
public class GroupCommandSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_open_id", nullable = false, length = 128)
    private String groupOpenId;

    @Column(name = "command_name", nullable = false, length = 64)
    private String commandName;

    @Column(nullable = false)
    private Boolean enabled;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
}
