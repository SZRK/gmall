package com.gmall.cart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gmall.cart.mapper.CartInfoMapper;
import com.gmall.cart.service.CartAsyncService;
import com.gmall.model.cart.CartInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class CartAsyncServiceImpl implements CartAsyncService {

    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Override
    @Async
    public void updateCart(CartInfo cartInfo) {
        cartInfoMapper.update(cartInfo, new QueryWrapper<CartInfo>()
                .eq("user_id", cartInfo.getUserId())
                .eq("sku_id", cartInfo.getSkuId()));
    }

    @Override
    @Async
    public void insertCart(CartInfo cartInfo) {
        cartInfoMapper.insert(cartInfo);
    }

    @Override
    @Async
    public void deleteCartInfoByUserTempIdAndSkuId(String userTempId, Long skuId) {
        cartInfoMapper.delete(new QueryWrapper<CartInfo>()
                .eq("user_id", userTempId)
                .eq("sku_id", skuId));

    }

    @Override
    @Async
    public void updateCartUserId(String userId, String userTempId) {
        CartInfo cartInfo = new CartInfo();
        cartInfo.setUserId(userId);
        cartInfoMapper.update(cartInfo, new QueryWrapper<CartInfo>()
                .eq("user_id", userTempId));
    }
}
