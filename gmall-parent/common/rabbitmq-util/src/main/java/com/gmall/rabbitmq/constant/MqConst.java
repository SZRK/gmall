package com.gmall.rabbitmq.constant;

/**
 * @author 李旭
 * @date 2021/12/3 14:35
 * @Description:
 *
 *
 *    消息的交换机 RoutingKey 队列   常量实现类
 */
public interface MqConst {

    /**
     * 商品上下架
     */
    public static final String EXCHANGE_DIRECT_GOODS = "yc.exchange.direct.goods";
    public static final String ROUTING_GOODS_UPPER = "goods.upper";
    public static final String ROUTING_GOODS_LOWER = "goods.lower";
    //队列
    public static final String QUEUE_GOODS_UPPER  = "yc.queue.goods.upper";
    public static final String QUEUE_GOODS_LOWER  = "yc.queue.goods.lower";

     // 取消订单，发送延迟队列
    public static final String EXCHANGE_DIRECT_ORDER_CANCEL = "yc.exchange.direct.order.cancel";//"exchange.direct.order.create" test_exchange;
    public static final String ROUTING_ORDER_CANCEL = "order.create";
    //延迟取消订单队列
    public static final String QUEUE_ORDER_CANCEL  = "yc.queue.order.cancel";
    //取消订单 延迟时间 单位：秒
    public static final int DELAY_TIME  = 2*60;


    public static final String EXCHANGE_DIRECT_PAYMENT_PAY = "yc.exchange.direct.payment.pay";
    public static final String ROUTING_PAYMENT_PAY = "yc.payment.pay";
    //队列
    public static final String QUEUE_PAYMENT_PAY  = "yc.queue.payment.pay";
    /**
     * 减库存
     */
    public static final String EXCHANGE_DIRECT_WARE_STOCK = "exchange.direct.ware.stock";
    public static final String ROUTING_WARE_STOCK = "ware.stock";
    //队列
    public static final String QUEUE_WARE_STOCK  = "queue.ware.stock";
    /**
     * 减库存成功，更新订单状态
     */
    public static final String EXCHANGE_DIRECT_WARE_ORDER = "exchange.direct.ware.order";
    public static final String ROUTING_WARE_ORDER = "ware.order";
    //队列
    public static final String QUEUE_WARE_ORDER  = "queue.ware.order";


    /**
     * 定时任务
     */
    public static final String EXCHANGE_DIRECT_TASK = "yc.exchange.direct.task";
    public static final String ROUTING_TASK_1 = "seckill.task.1";
    //队列
    public static final String QUEUE_TASK_1  = "yc.queue.task.1";

    /**
     * 秒杀
     */
    public static final String EXCHANGE_DIRECT_SECKILL_USER = "exchange.direct.seckill.user";
    public static final String ROUTING_SECKILL_USER = "seckill.user";
    //队列
    public static final String QUEUE_SECKILL_USER  = "queue.seckill.user";





}
