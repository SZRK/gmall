package com.gmall.web.controller;

import com.gmall.model.order.OrderInfo;
import com.gmall.order.client.OrderFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PaymentController {

    @Autowired
    private OrderFeignClient orderFeignClient;

    //  http://api.gmall.com/api/payment/alipay/submit/{orderId}(orderId=${orderInfo.id}
    @GetMapping("pay.html")
    public String toPayPage(Long orderId, Model model) {
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(String.valueOf(orderId));
        model.addAttribute("orderInfo", orderInfo);
        return "payment/pay";
    }
    @GetMapping("pay/success.html")
    public String toSuccessPage() {
        return "payment/success";
    }

}
