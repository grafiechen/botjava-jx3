package com.person.botjava.contants.channel;

/**
 * 权限类型
 * @author grafie.chen
 * @since 2024/7/12  16:01
 */

public enum PrivateType {
    OPEN(0,"公开频道"),
    MANAGER_ONLY(1,"群主管理员可见"),
    MANAGER_AND_MEMBER(2,"群主管理员+指定成员，可使用 修改子频道权限接口 指定成员")
    ;
    private final Integer code;
    private final String desc;

    PrivateType(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
