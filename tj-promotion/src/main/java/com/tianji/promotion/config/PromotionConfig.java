package com.tianji.promotion.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class PromotionConfig {
    @Bean
    public Executor generateExchangeCodeExecutor(){
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //核心线程池大小
        executor.setCorePoolSize(2);
        //2.最大线程池大小
        executor.setMaxPoolSize(5);
        //3.队列大小
        executor.setQueueCapacity(200);
        //4.线程名称
        executor.setThreadNamePrefix("exchange-code-handler-");
        //5.拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());//异步变同步

        executor.initialize();
        return executor;
    }
}
