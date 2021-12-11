package com.gmall.payment.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gmall.model.enums.PaymentStatus;
import com.gmall.model.enums.PaymentType;
import com.gmall.model.order.OrderInfo;
import com.gmall.model.payment.PaymentInfo;
import com.gmall.order.client.OrderFeignClient;
import com.gmall.payment.mapper.PaymentInfoMapper;
import com.gmall.payment.service.PaymentInfoService;
import com.gmall.rabbitmq.constant.MqConst;
import com.gmall.rabbitmq.service.RabbitMQService;
import com.google.common.collect.Ordering;
import org.aspectj.weaver.ast.Var;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.jws.Oneway;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@Service
public class PaymentInfoServcieImpl implements PaymentInfoService {


    @Autowired
    private OrderFeignClient orderFeignClient;

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private RabbitMQService rabbitMQService;

    // 插入支付表
    @Override
    public PaymentInfo insertPaymentInfo(String orderId, PaymentType paymentType) {

        // 先获取数据库中是否已经插入过当前订单对应的支付表
        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(new QueryWrapper<PaymentInfo>()
                .eq("order_id", orderId));
        if (null != paymentInfo) {
            // 数据库中已经有了记录则更改支付方式，及创建时间
            paymentInfo.setPaymentType(paymentType.name());
            paymentInfo.setCreateTime(new Date());
            paymentInfoMapper.updateById(paymentInfo);
        } else {
            // 没有数据则新增
            OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
            paymentInfo = new PaymentInfo();
            paymentInfo.setCreateTime(new Date());
            paymentInfo.setPaymentType(paymentType.name());
            paymentInfo.setOrderId(Long.parseLong(orderId));
            paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
            paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
            paymentInfo.setSubject(orderInfo.getTradeBody());
            paymentInfo.setPaymentStatus(PaymentStatus.UNPAID.name());
            // 新增到数据库
            paymentInfoMapper.insert(paymentInfo);
        }
        return paymentInfo;
    }

    // 更新支付表
    @Override
    public void update(Map<String, String> paramsMap) {

        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(new QueryWrapper<PaymentInfo>()
                .eq("out_trade_no", paramsMap.get("out_trade_no")));
        if(null != paymentInfo && PaymentStatus.UNPAID.name().equals(paymentInfo.getPaymentStatus())){
            //支付中 改已支付
            paymentInfo.setPaymentStatus(PaymentStatus.PAID.name());
            paymentInfo.setTradeNo(paramsMap.get("trade_no"));
            try {
                paymentInfo.setCallbackTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        .parse(paramsMap.get("notify_time")));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            paymentInfo.setCallbackContent(JSONObject.toJSONString(paramsMap));
            paymentInfoMapper.updateById(paymentInfo);
            //发消息  保存最终一致性 事务问题 更新订单表
            rabbitMQService.sentMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY,
                    MqConst.ROUTING_PAYMENT_PAY,paymentInfo.getOrderId());
            // 异步通知库存系统，判断是否需要减库存


        }
    }
}
