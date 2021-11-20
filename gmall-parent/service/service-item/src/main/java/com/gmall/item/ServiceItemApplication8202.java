package com.gmall.item;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
@EnableFeignClients(basePackages = "com.gmall")
@ComponentScan("com.gmall")
@EnableDiscoveryClient
public class ServiceItemApplication8202 {
    public static void main(String[] args) {
        SpringApplication.run(ServiceItemApplication8202.class,args);
    }
}
