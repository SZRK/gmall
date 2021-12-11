package com.gmall.order.listener;

import com.alibaba.fastjson.JSONObject;
import com.gmall.model.enums.OrderStatus;
import com.gmall.model.enums.ProcessStatus;
import com.gmall.model.order.OrderInfo;
import com.gmall.order.mapper.OrderInfoMapper;
import com.gmall.order.service.OrderService;
import com.gmall.rabbitmq.constant.MqConst;
import com.gmall.rabbitmq.service.RabbitMQService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class OrderRabbitMQListener {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderService orderService;

    @Autowired
    private RabbitMQService rabbitMQService;

    @RabbitListener(queues = MqConst.QUEUE_ORDER_CANCEL)
    public void cancelOrder(Message message, Channel channel, int orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        if (null != orderInfo && OrderStatus.UNPAID.name().equals(orderInfo.getOrderStatus())) {
            System.out.println("过期时间");
            // 半小时之后未支付， 取消支付, 修改数据库中的状态
            orderInfo.setOrderStatus(OrderStatus.CLOSED.name());
            orderInfo.setProcessStatus(ProcessStatus.CLOSED.name());
            orderInfoMapper.updateById(orderInfo);
        }
        try {
            // 回复消息确认消息
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            //   是否投递过  true:已经投递过了本次第二次投递   false:没有投递过 本次是第一次投递
            if (message.getMessageProperties().isRedelivered()) {
                //true:已经投递过了本次第二次投递
                System.out.println("本次是第二次投递了，不能再投递了");
                //拒绝： 将队列中的消息删除 才能让队列中的其它消息继续消费  打日志  转人工
                //参数1：消息的标识
                //参数2:是否放回队列   false：不放回队列（删除）, true:放回队列
                try {
                    channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } else {
                //false:没有投递过 本次是第一次投递
                System.out.println("没有投递过 本次是第一次投递");
                //参数1：消息的标识
                //参数2：是否批量应答
                //参数2:是否放回队列   false：不放回队列（删除）, true:放回队列
                try {
                    channel.basicNack(message.getMessageProperties().getDeliveryTag(),
                            false, true);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_PAYMENT_PAY,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_PAYMENT_PAY),
            key = {MqConst.ROUTING_PAYMENT_PAY}
    ))
    public void updateOrderInfo(Message message, Channel channel, String orderId) {
        try {
            orderService.updateOrderInfo(orderId, ProcessStatus.PAID);
            // 通知仓储扣减库存
            //组装仓需要的数据
            Map orderTaskJson = orderService.initWareData(orderId);//组合所需要的数据
            orderService.updateOrderInfo(orderId, ProcessStatus.NOTIFIED_WARE);
            //发消息给库存系统  扣减库存   最终一致性
            rabbitMQService.sentMessage(MqConst.EXCHANGE_DIRECT_WARE_STOCK,
                    MqConst.ROUTING_WARE_STOCK, JSONObject.toJSONString(orderTaskJson));
            // 回复消息确认消费
            System.out.println("扣减库存消息发送成功");
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            //   是否投递过  true:已经投递过了本次第二次投递   false:没有投递过 本次是第一次投递
            if (message.getMessageProperties().isRedelivered()) {
                //true:已经投递过了本次第二次投递
                System.out.println("本次是第二次投递了，不能再投递了");
                //拒绝： 将队列中的消息删除 才能让队列中的其它消息继续消费  打日志  转人工
                //参数1：消息的标识
                //参数2:是否放回队列   false：不放回队列（删除）, true:放回队列
                try {
                    channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } else {
                //false:没有投递过 本次是第一次投递
                System.out.println("没有投递过 本次是第一次投递");
                //参数1：消息的标识
                //参数2：是否批量应答
                //参数2:是否放回队列   false：不放回队列（删除）, true:放回队列
                try {
                    channel.basicNack(message.getMessageProperties().getDeliveryTag(),
                            false, true);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_WARE_ORDER,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_WARE_ORDER),
            key = {MqConst.ROUTING_WARE_ORDER}
    ))
    public void  receiverMessageDecrStock(Message message, Channel channel, String json) {
        System.out.println("接收到的消息：" + json);
        Map map = JSONObject.parseObject(json, Map.class);
            // 已减库存，更改为代发货
            if("DEDUCTED".equals(map.get("status"))){
                //扣减成功
                orderService.updateOrderInfo(
                        String.valueOf(map.get("orderId")),ProcessStatus.WAITING_DELEVER);
            }else{
                //库存超卖
                orderService.updateOrderInfo(
                        String.valueOf(map.get("orderId")),ProcessStatus.STOCK_EXCEPTION);
            }

    }
}
