package com.gmall.list.listener;

import com.gmall.list.service.ListYcService;
import com.gmall.rabbitmq.constant.MqConst;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RabbitListListener {

    @Autowired
    private ListYcService listYcService;

    @RabbitListener(bindings = {@QueueBinding(
            value = @Queue(value = MqConst.QUEUE_GOODS_UPPER,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_GOODS),
            key = {MqConst.ROUTING_GOODS_UPPER}
    )})
    public void onSale(Message message, Channel channel, String skuId) {
        try {
            listYcService.onSale(Long.parseLong(skuId));
            System.out.println("*************" +
                    message.getMessageProperties().getAppId());
            // 确认消息已被消费
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (IOException e) {
            e.printStackTrace();
            // 判断是否投递过
            if (!message.getMessageProperties().getRedelivered()) {
                // 消息已经投递过
                // 拒接继续投递
                try {
                    channel.basicReject(message.getMessageProperties().getDeliveryTag(),false);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } else{
                //false:没有投递过 本次是第一次投递
                System.out.println("没有投递过 本次是第一次投递");
                //参数1：消息的标识
                //参数2：是否批量应答
                //参数2:是否放回队列   false：不放回队列（删除）, true:放回队列
                try {
                    channel.basicNack(message.getMessageProperties().getDeliveryTag(),
                            false,true);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * 下架
     *
     * @param message
     * @param channel
     * @param skuId
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_GOODS_LOWER, durable = "true", autoDelete = "false"),
            exchange = @Exchange(MqConst.EXCHANGE_DIRECT_GOODS),
            key= {MqConst.ROUTING_GOODS_LOWER}
            ))
    public void cancelSale(Message message, Channel channel, String skuId) {
        try {
            listYcService.cancelSale(Long.parseLong(skuId));
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            e.printStackTrace();
            // 判断是否投递过
            if (message.getMessageProperties().getRedelivered()) {
                // 消息已经投递过
                // 拒接继续投递
                try {
                    channel.basicReject(message.getMessageProperties().getDeliveryTag(),false);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } else{
                //false:没有投递过 本次是第一次投递
                System.out.println("没有投递过 本次是第一次投递");
                //参数1：消息的标识
                //参数2：是否批量应答
                //参数2:是否放回队列   false：不放回队列（删除）, true:放回队列
                try {
                    channel.basicNack(message.getMessageProperties().getDeliveryTag(),
                            false,true);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

}
