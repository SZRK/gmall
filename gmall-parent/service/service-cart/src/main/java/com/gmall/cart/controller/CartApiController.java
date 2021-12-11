package com.gmall.cart.controller;

import com.gmall.cart.service.CartService;
import com.gmall.common.result.Result;
import com.gmall.common.util.AuthContextHolder;
import com.gmall.model.cart.CartInfo;
import org.apache.ibatis.annotations.Delete;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartApiController {

    @Autowired
    private CartService cartService;

    @PostMapping("/addToCart/{skuId}/{skuNum}")
    public CartInfo addToCart(@PathVariable Long skuId, @PathVariable Integer skuNum,
                              HttpServletRequest request) {
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)) {
            userId = AuthContextHolder.getUserTempId(request);
        }
        CartInfo cartInfo = cartService.addToCart(skuId, skuNum, userId);
        return cartInfo;
    }

    @GetMapping("/getCartInfo/{skuId}")
    public CartInfo getCartInfo(@PathVariable Long skuId, HttpServletRequest request) {
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)) {
            userId = AuthContextHolder.getUserTempId(request);
        }
        return cartService.getCartInfo(userId, skuId);
    }

    //获取购物车中的cartInfo集合
    @GetMapping("/cartList")
    public Result getCartList(HttpServletRequest httpServletRequest) {
        String userId = AuthContextHolder.getUserId(httpServletRequest);
        String userTempId = AuthContextHolder.getUserTempId(httpServletRequest);
        List<CartInfo> list = cartService.getcartList(userId, userTempId);
        return Result.ok(list);
    }

    // 选中和取消选中
    @GetMapping("/checkCart/{skuId}/{isChecked}")
    public Result checkCart(@PathVariable Long skuId, @PathVariable Integer isChecked,
                            HttpServletRequest request) {
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)) {
            userId = AuthContextHolder.getUserTempId(request);
        }
        cartService.checkCart(skuId, isChecked, userId);
        return Result.ok();
    }
    // 删除当前购物项
    @DeleteMapping("/deleteCart/{skuId}")
    public Result deleteCart(@PathVariable Long skuId, HttpServletRequest request) {
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)) {
            userId = AuthContextHolder.getUserTempId(request);
        }
        cartService.deleteCart(skuId, userId);
        return Result.ok();
    }

    // 获取购物车中的选中项
    @GetMapping("/getCartCheckedList/{userId}")
    public List<CartInfo> getCartCheckedList(@PathVariable String userId) {
        return cartService.getCartCheckedList(userId);
    }



}
