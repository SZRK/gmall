package com.gmall.order.service.impl;

import com.gmall.cart.client.CartFeignClient;
import com.gmall.model.order.OrderDetail;
import com.gmall.order.service.OrderAsyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderAsyncServiceImpl implements OrderAsyncService {

    @Autowired
    private CartFeignClient cartFeignClient;

    // 删除购物车中的购物项删除redis中的购物项
    @Override
    public void deleteCartList(List<OrderDetail> orderDetailList) {
        orderDetailList.stream().forEach(orderDetail -> {
            cartFeignClient.deleteCart(orderDetail.getSkuId());
        });

    }
}
