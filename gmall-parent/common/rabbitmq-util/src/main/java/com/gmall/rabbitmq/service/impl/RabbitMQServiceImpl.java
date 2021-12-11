package com.gmall.rabbitmq.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.gmall.common.constant.RedisConst;
import com.gmall.rabbitmq.entity.GmallCorrelationData;
import com.gmall.rabbitmq.service.RabbitMQService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.util.UUID;

// 用于发送消息
@Component
@Slf4j
public class RabbitMQServiceImpl implements RabbitMQService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedisTemplate redisTemplate;

    // 发送普通消息
    @Override
    public void sentMessage(String exchange, String routingKey, Object msg) {
        GmallCorrelationData gmallCorrelationData = new GmallCorrelationData();
        gmallCorrelationData.setExchange(exchange);
        gmallCorrelationData.setRoutingKey(routingKey);
        gmallCorrelationData.setMessage(msg);
        gmallCorrelationData.setRetryCount(0); // 重试次数
        // 交换机到队列回调中不能获取到消息信息, 因此需要借用redis来缓存当前消息的信息
        String rabbitCacheKey = UUID.randomUUID().toString().replaceAll("-", "");
        gmallCorrelationData.setId(rabbitCacheKey);
        // 将gmallCorrelationData放入缓存中(使用Hash类型)
        redisTemplate.opsForHash().put(RedisConst.RABBITMQ_CACHE_KEY, rabbitCacheKey,
                JSONObject.toJSONString(gmallCorrelationData));
        // MessagePostProcessor messagePostProcessor 设置消息持久化
        rabbitTemplate.convertAndSend(exchange, routingKey, msg, message -> {
            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.fromInt(2));
            return message;
        }, gmallCorrelationData);
    }

    // 发送延时消息
    @Override
    public void sentDelayedMessage(String exchange, String routingKey, Object msg, int delayTime) {
        GmallCorrelationData gmallCorrelationData = new GmallCorrelationData();
        gmallCorrelationData.setExchange(exchange);
        gmallCorrelationData.setRoutingKey(routingKey);
        gmallCorrelationData.setMessage(msg);
        gmallCorrelationData.setDelayTime(delayTime);
        gmallCorrelationData.setRetryCount(0); // 重试次数
        // 交换机到队列回调中不能获取到消息信息, 因此需要借用redis来缓存当前消息的信息
        String rabbitCacheKey = UUID.randomUUID().toString().replaceAll("-", "");
        gmallCorrelationData.setId(rabbitCacheKey);
        // 将gmallCorrelationData放入缓存中(使用Hash类型)
        redisTemplate.opsForHash().put(RedisConst.RABBITMQ_CACHE_KEY, rabbitCacheKey,
                JSONObject.toJSONString(gmallCorrelationData));
        // MessagePostProcessor messagePostProcessor 设置消息持久化
        rabbitTemplate.convertAndSend(exchange, routingKey, msg, message -> {
            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.fromInt(2));
            //message.getMessageProperties().setExpiration(String.valueOf(delayTime));
            return message;
        }, gmallCorrelationData);
    }



}
