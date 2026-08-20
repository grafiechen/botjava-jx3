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

import java.time.LocalDateTime;

/**
 * 群维度的主动推送任务订阅。不存在记录时按关闭处理。
 */
@Data
@Entity
@Table(name = "group_push_subscription", uniqueConstraints = @UniqueConstraint(
        name = "uk_group_push_subscription_group_task",
        columnNames = {"group_open_id", "task_code"}
))
public class GroupPushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_open_id", nullable = false, length = 128)
    private String groupOpenId;

    @Column(name = "task_code", nullable = false, length = 64)
    private String taskCode;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(name = "updated_by", length = 128)
    private String updatedBy;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
}
