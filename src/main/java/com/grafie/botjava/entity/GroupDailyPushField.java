package com.grafie.botjava.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "group_daily_push_field", uniqueConstraints = @UniqueConstraint(
        name = "uk_group_daily_push_field",
        columnNames = {"group_open_id", "mongo_field_name"}
))
public class GroupDailyPushField {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "group_open_id", nullable = false, length = 128)
    private String groupOpenId;
    @Column(name = "mongo_field_name", nullable = false, length = 100)
    private String mongoFieldName;
    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;
    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, length = 20)
    private ScriptStatusFieldDefinition.ValueType valueType = ScriptStatusFieldDefinition.ValueType.TEXT;
    @Column(nullable = false)
    private boolean enabled = true;
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
    @Column(name = "updated_by", length = 128)
    private String updatedBy;
    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;
    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
}