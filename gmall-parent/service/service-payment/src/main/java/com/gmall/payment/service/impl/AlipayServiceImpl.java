package com.gmall.payment.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.gmall.model.enums.PaymentType;
import com.gmall.model.payment.PaymentInfo;
import com.gmall.payment.config.MyAlipayConfig;
import com.gmall.payment.service.AlipayService;
import com.gmall.payment.service.PaymentInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AlipayServiceImpl implements AlipayService {

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private PaymentInfoService paymentInfoService;

    // 跳转支付宝扫码支付页面
    @Override
    public String submit(String orderId) {
        // 生成支付表
        PaymentInfo paymentInfo = paymentInfoService.insertPaymentInfo(orderId, PaymentType.ALIPAY);
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl(MyAlipayConfig.notify_payment_url); // 用于用户付款后通知商家用户已付款
        request.setReturnUrl(MyAlipayConfig.return_payment_url); // 通知付款用户付款成功后网页跳转路径
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", paymentInfo.getOutTradeNo());
        bizContent.put("total_amount", paymentInfo.getTotalAmount());
        bizContent.put("subject", paymentInfo.getSubject());
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
        request.setBizContent(bizContent.toString());
        AlipayTradePagePayResponse response = null;
        try {
            response = alipayClient.pageExecute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if (response.isSuccess()) {
            System.out.println("调用成功");
        } else {
            System.out.println("调用失败");
        }
        return response.getBody();
    }
}
