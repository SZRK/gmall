package com.gmall.activity.listenner;
import com.gmall.activity.redis.CacheHelper;
import com.gmall.activity.service.ActivityService;
import com.gmall.common.constant.RedisConst;
import com.gmall.model.activity.OrderRecode;
import com.gmall.model.activity.SeckillGoods;
import com.gmall.model.activity.UserRecode;
import com.gmall.rabbitmq.constant.MqConst;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
public class ActivityListenner {


    @Autowired
    private ActivityService activityService;

    @Autowired
    private RedisTemplate redisTemplate;


    // 接收上架秒杀商品的消息
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_TASK_1, durable = "true", autoDelete = "false"),
            exchange = @Exchange(MqConst.EXCHANGE_DIRECT_TASK),
            key = {MqConst.ROUTING_TASK_1}
    ))
    public void receiveTaskMessage(Message message, Channel channel, Object msg) {

        // 收到定时器发送的将秒杀物品上架的消息
        // 先检测当天的秒杀商品是否上架
        try {
            activityService.secKillOnSale();
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
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

    // 接收开始秒杀的消息
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_SECKILL_USER, durable = "true", autoDelete = "false"),
            exchange = @Exchange(MqConst.EXCHANGE_DIRECT_SECKILL_USER),
            key = MqConst.ROUTING_SECKILL_USER
    ))
    public void receiveSecKillMessage(Message message, Channel channel, UserRecode userRecode) {
         try {
             // 二次校验状态位, 从本地缓存中取出状态位

            String  status = (String) CacheHelper.get(userRecode.getSkuId().toString());
            if ("0".equals(status)) {
                // 商品已售罄
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
                return ;
            }
            // 校验用户是否来过 当前key存在则返回false
             Boolean comeIn = redisTemplate.opsForValue().setIfAbsent(RedisConst.SECKILL_USER
                     + userRecode.getUserId() + ":" + userRecode.getSkuId(), "comeIn");
            if (!comeIn) {
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
                return ;
            }
             // 校验库存 弹出一个秒杀商品，不为空则表示有库存
             String  skuId = (String) redisTemplate.opsForList().leftPop(RedisConst.SECKILL_STOCK_PREFIX
                     + userRecode.getSkuId());
            if (null == skuId) {
                // 没货了
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
                // 修改缓存中的值为skuId:0
                //更新状态位 利用redis中的发布订阅模式
                redisTemplate.convertAndSend("seckillpush", userRecode.getSkuId() + ":0");
                return ;
            }
             // 保存抢购资格
             // 封装抢购商品信息
             OrderRecode orderRecode = new OrderRecode();
             orderRecode.setUserId(userRecode.getUserId());
             SeckillGoods seckillGoods  = (SeckillGoods) redisTemplate.opsForHash().get(RedisConst.SECKILL_GOODS, userRecode.getSkuId().toString());
             orderRecode.setSeckillGoods(seckillGoods);
             orderRecode.setNum(1);
             // 保存抢购资格
             redisTemplate.opsForHash().put(RedisConst.SECKILL_ORDERS + userRecode.getUserId(), userRecode.getSkuId(), orderRecode);
             channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
         } catch (IOException e) {
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

}
