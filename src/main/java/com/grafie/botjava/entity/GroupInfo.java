package com.grafie.botjava.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * @author grafie.chen
 * @since 2025/1/22  16:28
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "group_info", uniqueConstraints = @UniqueConstraint(
        name = "uk_group_info_open_group_id",
        columnNames = "open_group_id"
))
public class GroupInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "group_id")
    private String groupId;
    @Column(name = "open_group_id")
    private String openGroupId;
    /**
     * 服务器名称
     */
    private String server;
    /**
     * 是否允许当前群使用正式查询指令，历史数据为空时按开启处理。
     */
    @Column(name = "commands_enabled")
    private Boolean commandsEnabled;
    /**
     * 是否允许当前群使用实验功能，历史数据为空时按关闭处理。
     */
    @Column(name = "experimental_enabled")
    private Boolean experimentalEnabled;
    /**
     * 是否授权机器人向当前群发送主动消息，历史数据默认关闭。
     */
    @Column(name = "active_messages_enabled")
    private Boolean activeMessagesEnabled;
    /**
     * QQ 平台是否允许向当前群发送主动消息，与群管理员设置的本地开关相互独立。
     */
    @Column(name = "active_messages_platform_allowed")
    private Boolean activeMessagesPlatformAllowed;
    /**
     * 最近一次群主动消息的共享频控占位时间。
     */
    @Column(name = "active_message_reserved_at")
    private Instant activeMessageReservedAt;
    /**
     * 当前频控占位标识，发送失败时用于只回滚本次占位。
     */
    @Column(name = "active_message_reservation_id", length = 36)
    private String activeMessageReservationId;
    /**
     * 服务器推送
     */
    @Column(name = "ws_server")
    private Integer wsServer;
    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
    @CreationTimestamp
    @Column(name = "update_time", updatable = true)
    private LocalDateTime updateTime;
}
