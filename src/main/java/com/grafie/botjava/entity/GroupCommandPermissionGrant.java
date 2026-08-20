package com.grafie.botjava.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "group_command_permission_grant", uniqueConstraints = @UniqueConstraint(
        name = "uk_group_command_permission_grant",
        columnNames = {"group_open_id", "member_openid", "permission_key"}
))
public class GroupCommandPermissionGrant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "group_open_id", nullable = false, length = 128)
    private String groupOpenId;
    @Column(name = "member_openid", nullable = false, length = 128)
    private String memberOpenId;
    @Column(name = "permission_key", nullable = false, length = 100)
    private String permissionKey;
    @Column(nullable = false)
    private boolean enabled = true;
    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;
    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
}