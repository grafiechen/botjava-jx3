package com.person.botjava.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 用户对象(User)
 * @author  grafie.chen
 * @since 2024/7/12  15:36
 */
@Data
public class User {
    /**
     * 用户 id
     */
    @JsonProperty("id")
    private String id;
    /**
     * 用户名
     */
    @JsonProperty("username")
    private String username;
    /**
     * 用户头像地址
     */
    @JsonProperty("avatar")
    private String avatar;
    /**
     * 是否是机器人
     */
    @JsonProperty("bot")
    private String bot;

    /**
     * 特殊关联应用的 openid，需要特殊申请并配置后才会返回。如需申请，请联系平台运营人员。
     */
    @JsonProperty("union_openid")
    private String unionOpenId;
    /**
     * 机器人关联的互联应用的用户信息，与union_openid关联的应用是同一个。如需申请，请联系平台运营人员。
     * #提示
     */
    @JsonProperty("union_user_account")
    private String unionUserAccount;
}
