package com.grafie.botjava.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author grafie.chen
 * @since 2025/1/22  16:28
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
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
}
