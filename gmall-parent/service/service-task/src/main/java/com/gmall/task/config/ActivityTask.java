package com.gmall.task.config;

import com.gmall.rabbitmq.constant.MqConst;
import com.gmall.rabbitmq.service.RabbitMQService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 定时秒杀商品放入缓存池中
@Component
@EnableScheduling
@Slf4j
public class ActivityTask {

    @Autowired
    private RabbitMQService rabbitMQService;

    @Scheduled(cron = "0 24 21 * * ?")
    // 发送消息通知秒杀微服务上架秒杀商品，消息内容随意
    public void sendActivityTask() {
        System.out.println("秒杀商品消息发送成功");
        rabbitMQService.sentMessage(MqConst.EXCHANGE_DIRECT_TASK, MqConst.ROUTING_TASK_1, "1");
    }



}
