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

import java.time.LocalDateTime;

/**
 * WS 主动推送事件去重占位。唯一键保证多实例并发时最多一个实例进入发送队列。
 */
@Data
@Entity
@Table(name = "push_event_receipt", uniqueConstraints = @UniqueConstraint(
        name = "uk_push_event_receipt_task_fingerprint",
        columnNames = {"task_code", "event_fingerprint"}
))
public class PushEventReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_code", nullable = false, length = 64)
    private String taskCode;

    @Column(name = "event_fingerprint", nullable = false, length = 64)
    private String eventFingerprint;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;
}