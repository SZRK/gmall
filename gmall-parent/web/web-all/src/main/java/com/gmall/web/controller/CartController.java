package com.gmall.web.controller;

import com.gmall.cart.client.CartFeignClient;
import com.gmall.model.cart.CartInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;

@Controller
public class CartController {

    @Autowired
    private CartFeignClient cartFeignClient;

    @RequestMapping("addCart.html")
    public String addCart(@RequestParam(name = "skuId") Long skuId,
                          @RequestParam(name = "skuNum") Integer skuNum,
                          HttpServletRequest requeste,
                          Model model) {

        // 接收网关传过来的userId
        String userId = requeste.getHeader("userId");
        CartInfo cartInfo = cartFeignClient.addToCart(skuId, skuNum);
        model.addAttribute("model", model);
        return "cart/addCart";
    }


}
