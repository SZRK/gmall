package com.gmall.order.service;

import com.gmall.model.enums.ProcessStatus;
import com.gmall.model.order.OrderDetail;
import com.gmall.model.order.OrderInfo;

import java.util.List;
import java.util.Map;

public interface OrderService {
    Map toTradePage(String userId);

    boolean checkStock(OrderDetail orderDetai);

    Long saveOrderInfo(OrderInfo orderInfo);

    OrderInfo getOrcderInfo(String orderId);

    void updateOrderInfo(String orderId, ProcessStatus processStatus);

    Map initWareData(OrderInfo orderInfo);

    Map initWareData(String orderId);

    List<OrderInfo> orderSplit(Long orderId, String wareSkuMap);
}
