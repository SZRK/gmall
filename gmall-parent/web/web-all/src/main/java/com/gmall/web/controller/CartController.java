package com.gmall.web.controller;

import com.gmall.cart.client.CartFeignClient;
import com.gmall.common.util.AuthContextHolder;
import com.gmall.model.cart.CartInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import javax.servlet.http.HttpServletRequest;

@Controller
public class CartController {

    @Autowired
    private CartFeignClient cartFeignClient;


    @RequestMapping("addCart.html")
    public String addCart(@RequestParam(name = "skuId") Long skuId,
                          @RequestParam(name = "skuNum") Integer skuNum,
                          RedirectAttributes redirectAttributes) {
        // 接收网关传过来的userId
        CartInfo cartInfo = cartFeignClient.addToCart(skuId, skuNum);
        redirectAttributes.addAttribute("skuId", skuId);
        redirectAttributes.addAttribute("skuNum", skuNum);
        return "redirect:http://cart.gmall.com/toCartPage.html";
    }

    @GetMapping("toCartPage.html")
    public String toCartPage(@RequestParam(name = "skuId") Long skuId,
                             @RequestParam(name = "skuNum") Integer skuNum,
                             Model model, HttpServletRequest request) {
        String userId = request.getHeader("userId");
        // 设置显示此次添加的商品数量
        CartInfo cartInfo = cartFeignClient.getCartInfo(skuId);
        cartInfo.setSkuNum(skuNum);

        model.addAttribute("cartInfo", cartInfo);
        return "cart/addCart";
    }

    @GetMapping("cart.html")
    public String toCart(HttpServletRequest request) {
        // 查出所有购物车集合
        return "cart/index";
    }

}
