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

import java.time.LocalDateTime;

/**
 * QQ 群成员在指定群内保存的常用角色。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "user_role_binding", uniqueConstraints = @UniqueConstraint(
        name = "uk_user_role_binding_group_account_role",
        columnNames = {"group_open_id", "member_openid", "server", "role_name"}
))
public class UserRoleBinding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_open_id", length = 128)
    private String groupOpenId;

    @Column(name = "member_openid", nullable = false, length = 128)
    private String memberOpenId;

    @Column(nullable = false, length = 64)
    private String server;

    @Column(name = "role_name", nullable = false, length = 64)
    private String roleName;

    @Column(length = 64)
    private String school;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;
}