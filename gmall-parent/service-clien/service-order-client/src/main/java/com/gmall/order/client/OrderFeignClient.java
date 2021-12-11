package com.gmall.order.client;

import com.gmall.common.result.Result;
import com.gmall.model.order.OrderInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient("service-orderyc")
public interface OrderFeignClient {

    @GetMapping("/api/order/auth/trade")
    Result<Map> toTradePage();

    @GetMapping("/api/order/auth/getOrderInfo/{orderId}")
    OrderInfo getOrderInfo(@PathVariable String orderId);

}
