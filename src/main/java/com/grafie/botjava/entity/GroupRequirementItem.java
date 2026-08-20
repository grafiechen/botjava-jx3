package com.grafie.botjava.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 群员追加到需求主题下的具体内容。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "group_requirement_item")
public class GroupRequirementItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requirement_id", nullable = false)
    private GroupRequirement requirement;

    @Column(name = "member_openid", length = 128)
    private String memberOpenId;

    @Column(name = "member_name", length = 100)
    private String memberName;

    @Column(columnDefinition = "text")
    private String content;

    @Column(length = 100)
    private String status;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;
}
