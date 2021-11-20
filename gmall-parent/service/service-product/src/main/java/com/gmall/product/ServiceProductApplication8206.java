package com.gmall.product;
import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan("com.gmall")
@EnableKnife4j
public class ServiceProductApplication8206 {
    public static void main(String[] args) {
        SpringApplication.run(ServiceProductApplication8206.class,args);
    }

}
