package com.grafie.botjava.service;

import com.grafie.botjava.jx3.http.util.REGEX;

import java.util.Optional;

/**
 * 群指令权限配置预留接口。
 *
 * <p>后续数据库实现负责定义哪些指令属于权限接口，以及指定群内哪些成员可操作。</p>
 */
public interface GroupCommandPermissionConfiguration {

    /**
     * 当前指令是否由权限配置接管。
     */
    boolean isPermissionCommand(REGEX command);

    /**
     * 评估指定群成员是否可操作权限指令。
     */
    Optional<Decision> evaluate(String groupOpenId, String memberOpenId, REGEX command);

    enum Decision {
        ALLOW,
        DENY;

        public boolean allowed() {
            return this == ALLOW;
        }
    }
}