package com.gmall.cart.client;

import com.gmall.common.result.Result;
import com.gmall.model.cart.CartInfo;
import com.gmall.model.order.OrderDetail;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.List;


@FeignClient("service-cartyc")
public interface CartFeignClient {
    @PostMapping("/api/cart/addToCart/{skuId}/{skuNum}")
    CartInfo addToCart(@PathVariable Long skuId, @PathVariable Integer skuNum);

    @GetMapping("/api/cart/getCartInfo/{skuId}")
    CartInfo getCartInfo(@PathVariable Long skuId);

    // 获取购物车中的选中项
    @GetMapping("/api/cart/getCartCheckedList/{userId}")
    List<CartInfo> getCartCheckedList(@PathVariable String userId);

    @DeleteMapping("/api/cart/deleteCart/{skuId}")
    Result deleteCart(@PathVariable Long skuId);
}
