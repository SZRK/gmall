package com.gmall.rabbitmq.config;

import com.alibaba.fastjson.JSONObject;
import com.gmall.common.constant.RedisConst;
import com.gmall.rabbitmq.entity.GmallCorrelationData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.PostConstruct;
import javax.management.monitor.GaugeMonitor;

@Configuration
@Slf4j
public class RabbitMQConfig implements RabbitTemplate.ReturnCallback,
        RabbitTemplate.ConfirmCallback{

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     *  @PostConstruct 当对象创建后悔调用被此注解修饰的方法对当前对象进行增强
     */
    @PostConstruct
    public void initField() {
        // 初始化RabbitTemplate中的ConfirmCallback和ReturnCallback字段
        rabbitTemplate.setConfirmCallback(this);
        rabbitTemplate.setReturnCallback(this);
    }

    /**
     * 生产者向交换机发送消息，交换机收到获取没有收到都会触发confirm回调
     *@param correlationData : 应答对象 （交换机、RoutingKey 、Message）
     * @param ack   应答状态  ack:true 成功  ack:false 失败
     * @param cause 应答原因   成功 cause是null   |   cause不是null  失败的原
     */
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {


        GmallCorrelationData gmallCorrelationData = (GmallCorrelationData) correlationData;
        if (!ack) {
            // 如果消息发送失败,就重试
            retrySendMessage(gmallCorrelationData);
            return;
        }
        log.info("交换机消息发送成功，消息为：{}", JSONObject.toJSONString(gmallCorrelationData));
    }

    /**
     *  交换机向队列发送消息，当队列没有收到是触发回调
     * @param message
     */
    @Override
    public void returnedMessage(Message message, int replyCode, String replyText,
                                String exchange, String routingKey) {
        String uuid = (String) message.getMessageProperties().getHeaders().get("spring_returned_message_correlation");
        String jsonData = (String) redisTemplate.opsForHash().get(RedisConst.RABBITMQ_CACHE_KEY, uuid);
        GmallCorrelationData gmallCorrelationData = JSONObject.parseObject(jsonData, GmallCorrelationData.class);
        retrySendMessage(gmallCorrelationData);
    }

    private void retrySendMessage(GmallCorrelationData gmallCorrelationData) {
        if (gmallCorrelationData.getRetryCount() >= 3) {
            // 不在发送消息
            log.error("重新发送的次数已经耗尽了,{}", JSONObject.toJSONString(gmallCorrelationData));
            return;
        }
        // 否则 消息发送次数 +1
        gmallCorrelationData.setRetryCount(gmallCorrelationData.getRetryCount() + 1);
        // 修改redis中的值
        redisTemplate.opsForHash().put(RedisConst.RABBITMQ_CACHE_KEY,
                gmallCorrelationData.getId(), JSONObject.toJSONString(gmallCorrelationData));
        // 重新发送信息
        log.warn("队列消息重新发送，消息为：{}",  JSONObject.toJSONString(gmallCorrelationData));
        rabbitTemplate.convertAndSend(gmallCorrelationData.getExchange(),
                gmallCorrelationData.getRoutingKey(),
                gmallCorrelationData.getMessage(),
                message -> {
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.fromInt(2));
                    return message; },
                gmallCorrelationData);
        return;
    }
}
