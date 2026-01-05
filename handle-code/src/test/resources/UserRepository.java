package com.yokior.test;

import org.springframework.stereotype.Repository;

/**
 * 用户仓储接口
 * 定义数据访问层操作
 *
 * @author Yokior
 * @version 1.0
 * @since 2026-01-05
 */
@Repository
public interface UserRepository {

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
    java.util.List<User> findAll();

    /**
     * 保存用户
     *
     * @param user 用户对象
     * @return 保存后的用户对象
     */
    User save(User user);

    /**
     * 删除用户
     *
     * @param id 用户ID
     * @return 删除结果
     */
    boolean deleteById(Long id);
}