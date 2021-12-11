package com.gmall.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients("com.gmall")
@ComponentScan("com.gmall")
public class PaymentApplication8205 {
    public static void main(String[] args) {
        SpringApplication.run(PaymentApplication8205.class, args);
    }
}
