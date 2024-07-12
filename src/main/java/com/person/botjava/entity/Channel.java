package com.person.botjava.entity;

import lombok.Data;

/**
 * 子频道对象(Channel)
 * @author grafie.chen
 * @since 2024/7/12  15:48
 */
@Data
public class Channel {
    private String id;
    private String guildId;
    private String name;
    private Integer type;
    private Integer subType;
    private Integer position;
    private String parentId;
    private String ownerId;
    private Integer privateType;
    private Integer speakPermission;
    private String applicationId;
    private String permissions;
}
