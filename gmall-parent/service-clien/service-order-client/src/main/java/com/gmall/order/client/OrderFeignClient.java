package com.gmall.order.client;

import com.gmall.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@FeignClient("service-orderyc")
public interface OrderFeignClient {

    @GetMapping("/api/order/auth/trade")
    Result<Map> toTradePage();
}
