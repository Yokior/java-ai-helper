package com.yokior.test;

/**
 * 用户状态枚举
 * 定义用户的各种状态
 *
 * @author Yokior
 * @version 1.0
 * @since 2026-01-05
 */
public enum UserStatus {

    /**
     * 活跃状态
     */
    ACTIVE("活跃", 1),

    /**
     * 非活跃状态
     */
    INACTIVE("非活跃", 2),

    /**
     * 已删除状态
     */
    DELETED("已删除", 3),

    /**
     * 已封禁状态
     */
    BANNED("已封禁", 4);

    private final String description;
    private final int code;

    UserStatus(String description, int code) {
        this.description = description;
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public int getCode() {
        return code;
    }

    /**
     * 根据代码获取状态
     *
     * @param code 状态代码
     * @return 用户状态
     */
    public static UserStatus fromCode(int code) {
        for (UserStatus status : values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid status code: " + code);
    }

    @Override
    public String toString() {
        return String.format("UserStatus{code=%d, description='%s'}", code, description);
    }
}