package com.yokior.test;

import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 用户服务类 - 包含以get开头但不是getter的方法
 * 用于测试智能过滤逻辑
 *
 * @author Yokior
 * @version 1.0
 * @since 2026/01/05
 */
@Service
public class UserServiceWithGetMethod {

    private String name;
    private int age;
    private UserRepository userRepository;

    // 普通的getter方法 - 应该被过滤
    public String getName() {
        return name;
    }

    // 普通的setter方法 - 应该被过滤
    public void setName(String name) {
        this.name = name;
    }

    // 普通的getter方法 - 应该被过滤
    public int getAge() {
        return age;
    }

    // 普通的setter方法 - 应该被过滤
    public void setAge(int age) {
        this.age = age;
    }

    /**
     * 获取用户数据 - 这是一个业务方法，虽然以get开头但不是getter
     * 应该被保留
     *
     * @param userId 用户ID
     * @return 用户数据列表
     */
    public List<String> getUserData(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        // 复杂的业务逻辑
        return userRepository.findUserData(userId);
    }

    /**
     * 获取用户统计信息 - 业务方法，应该保留
     *
     * @return 统计信息
     */
    public String getUserStatistics() {
        // 复杂的统计逻辑
        int totalUsers = userRepository.countUsers();
        int activeUsers = userRepository.countActiveUsers();
        return String.format("总用户数: %d, 活跃用户数: %d", totalUsers, activeUsers);
    }

    /**
     * 设置用户配置 - 有复杂逻辑的setter，应该保留
     *
     * @param config 配置信息
     */
    public void setUserConfig(String config) {
        if (config == null || config.trim().isEmpty()) {
            throw new IllegalArgumentException("配置不能为空");
        }
        // 解析配置并设置
        String[] parts = config.split(",");
        if (parts.length != 2) {
            throw new IllegalArgumentException("配置格式错误");
        }
        this.name = parts[0];
        this.age = Integer.parseInt(parts[1]);
    }

    // toString方法 - 应该被过滤
    @Override
    public String toString() {
        return "UserServiceWithGetMethod{" +
                "name='" + name + '\'' +
                ", age=" + age +
                '}';
    }
}