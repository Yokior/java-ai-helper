package com.yokior.test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 抽象基础服务类
 * 提供通用的服务功能
 *
 * @author Yokior
 * @version 1.0
 * @since 2026-01-05
 */
public abstract class AbstractBaseService<T> {

    protected String serviceName;
    protected final List<T> cache;
    protected LocalDateTime lastUpdateTime;

    public AbstractBaseService(String serviceName) {
        this.serviceName = serviceName;
        this.cache = new ArrayList<>();
        this.lastUpdateTime = LocalDateTime.now();
    }

    /**
     * 抽象方法：获取实体ID
     *
     * @param entity 实体对象
     * @return 实体ID
     */
    protected abstract Long getEntityId(T entity);

    /**
     * 抽象方法：验证实体
     *
     * @param entity 实体对象
     * @return 验证结果
     */
    protected abstract boolean validateEntity(T entity);

    /**
     * 通用保存方法
     *
     * @param entity 实体对象
     * @return 保存结果
     */
    public boolean save(T entity) {
        if (!validateEntity(entity)) {
            return false;
        }
        cache.add(entity);
        lastUpdateTime = LocalDateTime.now();
        return true;
    }

    /**
     * 根据ID查找
     *
     * @param id 实体ID
     * @return 实体对象
     */
    public T findById(Long id) {
        return cache.stream()
                .filter(entity -> getEntityId(entity).equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取缓存大小
     *
     * @return 缓存中的实体数量
     */
    public int getCacheSize() {
        return cache.size();
    }

    /**
     * 清空缓存
     */
    public void clearCache() {
        cache.clear();
        lastUpdateTime = LocalDateTime.now();
    }
}