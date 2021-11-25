package com.gmall.list;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
@ComponentScan("com.gmall")
@EnableFeignClients(basePackages = "com.gmall")
@EnableDiscoveryClient
public class ListYcApplication8203 {
    public static void main(String[] args) {
        SpringApplication.run(ListYcApplication8203.class, args);
    }
}
