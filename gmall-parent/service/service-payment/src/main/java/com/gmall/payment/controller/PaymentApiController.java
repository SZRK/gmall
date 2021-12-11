package com.gmall.payment.controller;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.gmall.model.order.OrderInfo;
import com.gmall.order.client.OrderFeignClient;
import com.gmall.payment.config.MyAlipayConfig;
import com.gmall.payment.service.AlipayService;
import com.gmall.payment.service.PaymentInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.filter.OrderedFilter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("api/payment/alipay")
public class PaymentApiController {

    @Autowired
    private AlipayService alipayService;

    @Autowired
    private PaymentInfoService paymentInfoService;

    @GetMapping("submit/{orderId}")
    @ResponseBody
    public String savePaymentInfo(@PathVariable String orderId) {
        return alipayService.submit(orderId);
    }

    @GetMapping("/callback/return")
    public String returnCallBack() {
        return "redirect:" + MyAlipayConfig.return_order_url;
    }

    @PostMapping("/callback/notify")
    public String notifyCallBack(@RequestParam Map<String, String> paramsMap) {
        boolean signVerified = false; //调用SDK验证签名
        try {
            signVerified = AlipaySignature.rsaCheckV1(paramsMap, MyAlipayConfig.alipay_public_key,
                    MyAlipayConfig.charset, MyAlipayConfig.sign_type);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(signVerified){
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            // 判断状态是否为支付成功 成功则：1.更改订单状态 2.更改支付表状态 3.通知库存
            if("TRADE_SUCCESS".equals(paramsMap.get("trade_status"))) {
                // 支付成功
                paymentInfoService.update(paramsMap);
            }

            return "success";
        }else{
            // TODO 验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }
    }






}
