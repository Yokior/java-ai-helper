package com.yokior.test;

import java.util.List;
import java.util.Optional;

/**
 * 用户服务接口
 * 定义用户相关的核心操作
 *
 * @author Yokior
 * @version 1.0
 * @since 2026-01-05
 */
@FunctionalInterface
public interface IUserService {

    /**
     * 根据ID查找用户
     *
     * @param id 用户ID
     * @return 用户对象
     */
    User findById(Long id);

    /**
     * 获取所有用户
     *
     * @return 用户列表
     */
    List<User> findAll();

    /**
     * 保存用户
     *
     * @param user 用户对象
     * @return 保存后的用户对象
     */
    default User save(User user) {
        System.out.println("Default save implementation");
        return user;
    }

    /**
     * 根据名称查找用户
     *
     * @param name 用户名
     * @return 用户对象
     */
    static Optional<User> findByName(String name) {
        System.out.println("Static findByName implementation");
        return Optional.empty();
    }

    /**
     * 用户服务常量
     */
    String SERVICE_NAME = "UserService";
    int MAX_USERS = 1000;
}