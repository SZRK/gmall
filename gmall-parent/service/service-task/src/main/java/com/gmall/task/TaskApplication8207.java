package com.gmall.task;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)

@ComponentScan("com.gmall")
@EnableDiscoveryClient
public class TaskApplication8207 {
    public static void main(String[] args) {
        SpringApplication.run(TaskApplication8207.class, args);
    }
}
