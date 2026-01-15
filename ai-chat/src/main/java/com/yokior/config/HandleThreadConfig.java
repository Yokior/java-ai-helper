package com.yokior.config;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Yokior
 * @description
 * @date 2026/1/15 16:11
 */
@Configuration
public class HandleThreadConfig {


    @Bean("handleExpireConversation")
    public Executor handleExpireConversation() {
        return new ThreadPoolExecutor(
                1, // 核心线程数
                2, // 最大线程数
                0L, TimeUnit.MILLISECONDS, // 空闲线程存活时间
                new LinkedBlockingQueue<>(100), // 线程队列
                new ThreadFactoryBuilder().setNameFormat("handle-expire-conversation-%d").build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

}
