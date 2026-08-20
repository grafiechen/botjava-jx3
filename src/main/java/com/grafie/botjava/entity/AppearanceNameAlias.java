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
 * 全局外观名称别名，一条记录表示一个别名指向一个正式外观名称。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "appearance_name_alias", uniqueConstraints = @UniqueConstraint(
        name = "uk_appearance_name_alias_group_alias",
        columnNames = {"group_open_id", "normalized_alias_name"}
))
public class AppearanceNameAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_open_id", nullable = false, length = 128)
    private String groupOpenId;

    @Column(name = "canonical_name", nullable = false, length = 100)
    private String canonicalName;

    @Column(name = "normalized_canonical_name", nullable = false, length = 100)
    private String normalizedCanonicalName;

    @Column(name = "alias_name", nullable = false, length = 100)
    private String aliasName;

    @Column(name = "normalized_alias_name", nullable = false, length = 100)
    private String normalizedAliasName;

    @Column(name = "review_status", nullable = false, length = 20)
    private String reviewStatus;

    @Column(name = "creator_member_openid", length = 128)
    private String creatorMemberOpenId;

    @Column(name = "creator_name", length = 100)
    private String creatorName;

    @Column(name = "reviewer_member_openid", length = 128)
    private String reviewerMemberOpenId;

    @Column(name = "reviewer_name", length = 100)
    private String reviewerName;

    @Column(name = "review_time")
    private LocalDateTime reviewTime;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
}
