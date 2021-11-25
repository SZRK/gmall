package com.gmall.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ThreadPoolExecutorConfig {

    @Bean("threadPoolExecutor")
    public ThreadPoolExecutor getThreadPoolExecutor() {
        return new ThreadPoolExecutor(5,// 常驻核心线程数
                10,// 最大工作线程数
                2, // 没有工作的线程的存活时间
                TimeUnit.SECONDS, // 存活时间的单位
                new ArrayBlockingQueue<>(5), // 阻塞队列
                Executors.defaultThreadFactory(), // 创建线程的工厂
                new ThreadPoolExecutor.AbortPolicy());// 拒接策略;
    }
}
