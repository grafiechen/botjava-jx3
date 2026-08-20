package com.grafie.botjava.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "bot_admin_audit_log")
public class BotAdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String category;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(name = "target_key", length = 128)
    private String targetKey;

    @Column(name = "actor_openid", length = 128)
    private String actorOpenid;

    @Column(name = "actor_source", length = 32)
    private String actorSource;

    @Column(nullable = false)
    private Boolean success;

    @Column(name = "failure_summary", length = 256)
    private String failureSummary;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;
}