package com.gmall.cart.controller;

import com.gmall.cart.service.CartService;
import com.gmall.common.result.Result;
import com.gmall.model.cart.CartInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/cart")
public class CartApiController {

    @Autowired
    private CartService cartService;

    @PostMapping("addToCart/{skuId}/{skuNum}")
    public CartInfo addToCart(@PathVariable Long skuId, @PathVariable Integer skuNum,
                              HttpServletRequest request) {
        String userId = request.getHeader("userId");
        CartInfo cartInfo = cartService.addToCart(skuId, skuNum, userId);
        return cartInfo;
    }
}
