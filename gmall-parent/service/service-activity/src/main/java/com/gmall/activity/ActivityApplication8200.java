package com.gmall.activity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.gmall")
@EnableFeignClients("com.gmall")
@EnableDiscoveryClient
public class ActivityApplication8200 {
    public static void main(String[] args) {
        SpringApplication.run(ActivityApplication8200.class, args);
    }
}
