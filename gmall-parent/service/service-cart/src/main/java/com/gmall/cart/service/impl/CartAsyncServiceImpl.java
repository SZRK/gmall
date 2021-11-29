package com.gmall.cart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gmall.cart.mapper.CartInfoMapper;
import com.gmall.cart.service.CartAsyncService;
import com.gmall.model.cart.CartInfo;
import com.gmall.model.user.UserInfo;
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
}
