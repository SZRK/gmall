package com.gmall.payment.service;

import com.gmall.model.enums.PaymentType;
import com.gmall.model.payment.PaymentInfo;

import java.util.Map;

public interface PaymentInfoService {
    PaymentInfo insertPaymentInfo(String orderId, PaymentType alipay);

    void update(Map<String, String> paramsMap);
}
