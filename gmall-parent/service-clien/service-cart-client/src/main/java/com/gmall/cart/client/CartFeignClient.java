package com.gmall.cart.client;

import com.gmall.model.cart.CartInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.http.HttpServletRequest;

@FeignClient("service-cartyc")
public interface CartFeignClient {
    @PostMapping("/api/cart/addToCart/{skuId}/{skuNum}")
    public CartInfo addToCart(@PathVariable Long skuId, @PathVariable Integer skuNum);
}
