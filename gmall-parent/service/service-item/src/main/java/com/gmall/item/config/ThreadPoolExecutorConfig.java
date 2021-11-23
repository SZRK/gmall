package com.gmall.item.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

@Configuration
public class ThreadPoolExecutorConfig {

    @Bean("threadPoolExecutor")
    public ThreadPoolExecutor getThreadPoolExecutor() {
        Executors.newScheduledThreadPool(100);
        return new ThreadPoolExecutor(5,// 常驻核心线程数
                10,// 最大工作线程数
                2, // 没有工作的线程的存活时间
                TimeUnit.SECONDS, // 存活时间的单位
                new ArrayBlockingQueue<>(5), // 阻塞队列
                Executors.defaultThreadFactory(), // 创建线程的工厂
                new ThreadPoolExecutor.AbortPolicy());// 拒接策略;
    }
}
