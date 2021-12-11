package com.gmall.rabbitmq.service;

public interface RabbitMQService {
  void sentMessage(String exchange, String routingKey, Object msg);
  void sentDelayedMessage(String exchange, String routingKey, Object msg, int delayTime);
}
