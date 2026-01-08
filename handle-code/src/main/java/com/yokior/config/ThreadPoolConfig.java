package com.yokior.config;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

@Configuration
public class ThreadPoolConfig {

    @Bean("codeProcessExecutor") // 给线程池起个名字，方便后续注入
    public Executor codeProcessExecutor() {
        int corePoolSize = Runtime.getRuntime().availableProcessors(); // 核心线程数，建议等于CPU核心数
        int maxPoolSize = corePoolSize * 2; // 最大线程数
        
        // 使用有界队列，防止任务堆积过多导致OOM
        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(1000);

        // 自定义线程名前缀，方便在日志中排查问题
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("code-process-executor-%d").build();

        // 拒绝策略：由调用者线程（主线程）执行该任务
        // 这样当线程池忙时，主流程会稍微变慢，但不会丢任务
        RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();

        return new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                60L, TimeUnit.SECONDS,
                workQueue,
                namedThreadFactory,
                handler
        );
    }

    @Bean("insertExecutor")
    public Executor insertExecutor() {
        return new ThreadPoolExecutor(
                1, // 核心线程数
                1, // 最大线程数
                0L, TimeUnit.MILLISECONDS, // 空闲线程存活时间
                new LinkedBlockingQueue<>(100), // 任务队列
                new ThreadFactoryBuilder().setNameFormat("insert-executor-%d").build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
