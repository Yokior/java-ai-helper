package com.yokior.test;

import java.time.LocalDateTime;

/**
 * 用户实体类
 * 记录用户的基本信息
 *
 * @author Yokior
 * @version 1.0
 * @since 2026-01-05
 */
public class User {

    private Long id;
    private String name;
    private String email;
    private UserStatus status;
    private LocalDateTime createTime;

    public User() {
        this.status = UserStatus.ACTIVE;
        this.createTime = LocalDateTime.now();
    }

    public User(Long id, String name, String email) {
        this();
        this.id = id;
        this.name = name;
        this.email = email;
    }

    /**
     * 静态工厂方法创建用户
     *
     * @param name 用户名
     * @param email 邮箱
     * @return 用户对象
     */
    public static User create(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        return user;
    }

    // Getter和Setter方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", status=" + status +
                ", createTime=" + createTime +
                '}';
    }
}