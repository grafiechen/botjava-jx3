package com.person.botjava.entity;

import lombok.Data;

import java.util.List;

/**
 * @author grafie.chen
 * @since 2024/7/12  15:38
 */
@Data
public class Member {
    private User user;
    private String nick;
    private List<String> roles;
    private String joinedAt;
}
