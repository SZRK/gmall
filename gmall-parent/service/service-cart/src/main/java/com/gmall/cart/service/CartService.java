package com.gmall.cart.service;

import com.gmall.model.cart.CartInfo;

import java.util.List;
import java.util.Map;

public interface CartService {
    CartInfo addToCart(Long skuId, Integer skuNum, String userId);

    CartInfo getCartInfo(String userId, Long skuId);

    List<CartInfo> getcartList(String userId, String userTempId);

    void checkCart(Long skuId, Integer isChecked, String userId);

    void deleteCart(Long skuId, String userId);

    List<CartInfo> getCartCheckedList(String userId);
}
