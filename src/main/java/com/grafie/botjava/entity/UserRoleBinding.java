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
 * QQ 账号保存的常用角色。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "user_role_binding", uniqueConstraints = @UniqueConstraint(
        name = "uk_user_role_binding_account_role",
        columnNames = {"member_openid", "server", "role_name"}
))
public class UserRoleBinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_openid", nullable = false, length = 128)
    private String memberOpenId;

    @Column(nullable = false, length = 64)
    private String server;

    @Column(name = "role_name", nullable = false, length = 64)
    private String roleName;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;
}
