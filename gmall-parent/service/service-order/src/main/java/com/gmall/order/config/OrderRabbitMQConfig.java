package com.gmall.order.config;

import com.gmall.rabbitmq.constant.MqConst;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class OrderRabbitMQConfig {

    @Bean
    public Exchange delayExchange() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-delayed-type", "direct");
        return new CustomExchange(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL,
                "x-delayed-message", true, false, arguments);
    }

    @Bean
    public Queue delayQueue() {
        return QueueBuilder.durable(MqConst.QUEUE_ORDER_CANCEL).build();
    }

    @Bean
    public Binding bindingDelayQueueToDelayExchange(@Qualifier("delayExchange") Exchange exchange,
                                                    @Qualifier("delayQueue") Queue queue) {
        return BindingBuilder.bind(queue).to(exchange).with(MqConst.ROUTING_ORDER_CANCEL).noargs();
    }
}
