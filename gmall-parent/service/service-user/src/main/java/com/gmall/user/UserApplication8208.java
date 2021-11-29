package com.gmall.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan("com.gmall")
public class UserApplication8208 {
    public static void main(String[] args) {
        SpringApplication.run(UserApplication8208.class,args);
    }
}
