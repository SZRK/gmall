package com.gmall.cart.service;

import com.gmall.model.cart.CartInfo;

public interface CartService {
    CartInfo addToCart(Long skuId, Integer skuNum, String userId);
}
