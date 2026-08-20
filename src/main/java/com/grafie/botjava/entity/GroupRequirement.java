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
 * QQ 群内创建的需求主题。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "group_requirement", uniqueConstraints = @UniqueConstraint(
        name = "uk_group_requirement_group_title",
        columnNames = {"group_open_id", "title"}
))
public class GroupRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_open_id", nullable = false, length = 128)
    private String groupOpenId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "creator_member_openid", length = 128)
    private String creatorMemberOpenId;

    @Column(name = "creator_name", length = 100)
    private String creatorName;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
}
