package com.pkg.littlewriter.config;

import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync(mode= AdviceMode.ASPECTJ)
public class AsyncConfig {
    private static final int CORE_POOL_SIZE = 2;
    private static final int MAX_POOL_SIZE = 22;
    private static final int QUEUE_CAPACITY = 4;

    @Bean(name="asyncExecutor")
    public Executor asyncThreadTaskExecutor() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(CORE_POOL_SIZE);
        threadPoolTaskExecutor.setMaxPoolSize(MAX_POOL_SIZE);
        threadPoolTaskExecutor.setQueueCapacity(QUEUE_CAPACITY);
        threadPoolTaskExecutor.setThreadNamePrefix("thread-pool");
        return threadPoolTaskExecutor;
    }
}