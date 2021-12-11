package com.gmall.order.service;

import com.gmall.model.order.OrderDetail;

import java.util.List;

public interface OrderAsyncService {
    void deleteCartList(List<OrderDetail> orderDetailList);
}
