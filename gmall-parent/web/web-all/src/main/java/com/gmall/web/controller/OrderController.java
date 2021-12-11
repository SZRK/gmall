package com.gmall.web.controller;

import com.gmall.common.result.Result;
import com.gmall.order.client.OrderFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Controller
public class OrderController {
    @Autowired
    private OrderFeignClient orderFeignClient;

    @GetMapping("trade.html")
    public String toTradePage(Model model, HttpServletRequest request) {
        Result<Map> result = orderFeignClient.toTradePage();
        model.addAllAttributes(result.getData());
        return "order/trade";
    }
    @GetMapping("success.html")
    public String toSuccessPage(Model model, HttpServletRequest request) {
        return "order/succcess";
    }

}
