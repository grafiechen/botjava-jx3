package com.grafie.botjava.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * @author grafie.chen
 * @since 2025/1/22  16:28
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "group_info")
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
