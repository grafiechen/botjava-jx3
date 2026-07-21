package com.grafie.botjava.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 群指令调用记录，不保存原始聊天内容和请求参数。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "command_invocation")
public class CommandInvocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invocation_id", nullable = false, unique = true, length = 16)
    private String invocationId;

    @Column(name = "group_openid", nullable = false, length = 128)
    private String groupOpenId;

    @Column(name = "member_openid", length = 128)
    private String memberOpenId;

    @Column(name = "command_name", nullable = false, length = 64)
    private String commandName;

    @Column(name = "command_group", nullable = false, length = 32)
    private String commandGroup;

    @Column(name = "external_call", nullable = false)
    private boolean externalCall;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CommandInvocationStatus status;

    @Column(name = "response_type", length = 32)
    private String responseType;

    @Column(name = "elapsed_ms", nullable = false)
    private long elapsedMillis;

    @Column(name = "failure_summary", length = 500)
    private String failureSummary;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;
}
