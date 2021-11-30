package com.gmall.cart.service;

import com.gmall.model.cart.CartInfo;

public interface CartAsyncService {
    void updateCart(CartInfo cartInfo);

    void insertCart(CartInfo cartInfo);

    void deleteCartInfoByUserTempIdAndSkuId(String userTempId, Long skuId);

    void updateCartUserId(String userId, String userTempId);
}
