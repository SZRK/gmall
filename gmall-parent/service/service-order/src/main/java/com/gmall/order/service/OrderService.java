package com.gmall.order.service;

import com.gmall.model.order.OrderDetail;
import com.gmall.model.order.OrderInfo;

import java.util.List;
import java.util.Map;

public interface OrderService {
    Map toTradePage(String userId);

    boolean checkStock(OrderDetail orderDetai);

    Long saveOrderInfo(OrderInfo orderInfo);
}
