package com.person.botjava.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 频道对象(Guild)
 * @author grafie.chen
 * @since 2024/7/12  15:44
 */
@Data
public class Guild {
    /**
     * 频道ID
     */
    @JsonProperty("id")
    private String id;
    /**
     * 频道名称
     */
    @JsonProperty("name")
    private String name;
    /**
     * 频道头像地址
     */
    @JsonProperty("icon")
    private String icon;
    /**
     * 创建人用户ID
     */
    @JsonProperty("owner_id")
    private String ownerId;
    /**
     * 当前人是否是创建人
     */
    @JsonProperty("owner")
    private Boolean owner;
    /**
     * 成员数
     */
    @JsonProperty("member_count")
    private Integer memberCount;
    /**
     * 最大成员数
     */
    @JsonProperty("max_members")
    private Integer maxMembers;
    /**+
     * 描述
     */
    @JsonProperty("description")
    private String description;
    /**
     * 加入时间
     */
    @JsonProperty("joined_at")
    private String joinedAt;
}
