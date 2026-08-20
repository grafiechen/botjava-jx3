package com.grafie.botjava.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "script_status_field", uniqueConstraints = @UniqueConstraint(
        name = "uk_script_status_field_mongo_name",
        columnNames = "mongo_field_name"
))
public class ScriptStatusFieldDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mongo_field_name", nullable = false, length = 100)
    private String mongoFieldName;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "group_name", nullable = false, length = 100)
    private String groupName;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, length = 20)
    private ValueType valueType = ValueType.TEXT;

    @Column(nullable = false)
    private boolean writable;

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

    public enum ValueType {
        TEXT,
        INTEGER,
        DECIMAL,
        BOOLEAN,
        DATETIME
    }
}