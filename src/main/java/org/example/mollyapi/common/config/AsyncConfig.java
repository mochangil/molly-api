package org.example.mollyapi.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "preProcessOrderExecutor")
    public Executor preProcessOrderExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("preProcessOrderExecutor Async-");
        // 설정 적용
        executor.initialize();
        return executor;
    }

    @Bean(name = "processOrderExecutor")
    public Executor processOrderExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("processOrderExecutor Async-");
        // 설정 적용
        executor.initialize();
        return executor;
    }

    @Bean(name = "paymentExecutor")
    public Executor paymentExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("paymentExecutor Async-");
        // 설정 적용
        executor.initialize();
        return executor;
    }
}

