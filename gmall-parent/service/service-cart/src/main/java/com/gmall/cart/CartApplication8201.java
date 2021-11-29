package com.gmall.cart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan("com.gmall")
@EnableFeignClients(basePackages = "com.gmall")
@EnableAsync
public class CartApplication8201 {
    public static void main(String[] args) {
        SpringApplication.run(CartApplication8201.class, args);
    }

}
