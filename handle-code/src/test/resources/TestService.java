package com.yokior.test;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

/**
 * 用户服务接口实现类
 * 用于处理用户相关的业务逻辑
 *
 * @author Yokior
 * @version 1.0
 * @since 2026-01-05
 */
@Service
public class TestService implements IUserService {

    @Autowired
    private UserRepository userRepository;

    private String serviceName;

    public TestService() {
        this.serviceName = "TestService";
    }

    public TestService(String serviceName) {
        this.serviceName = serviceName;
    }

    /**
     * 根据ID查找用户
     *
     * @param id 用户ID
     * @return 用户对象
     */
    @Override
    public User findById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid user ID");
        }
        return userRepository.findById(id);
    }

    /**
     * 获取所有用户
     *
     * @return 用户列表
     */
    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    /**
     * 保存用户
     *
     * @param user 用户对象
     * @return 保存后的用户对象
     */
    public User save(User user) {
        validateUser(user);
        return userRepository.save(user);
    }

    private void validateUser(User user) {
        if (user.getName() == null || user.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("User name cannot be empty");
        }
    }

    /**
     * 内部类：用户验证器
     */
    private class UserValidator {
        public boolean isValid(User user) {
            return user != null && user.getName() != null;
        }
    }
}