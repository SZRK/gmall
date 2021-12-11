package com.gmall.order.controller;

import com.alibaba.fastjson.JSONObject;
import com.gmall.common.result.Result;
import com.gmall.common.util.AuthContextHolder;
import com.gmall.common.util.HttpClient;
import com.gmall.common.util.HttpClientUtil;
import com.gmall.model.order.OrderDetail;
import com.gmall.model.order.OrderInfo;
import com.gmall.order.service.OrderAsyncService;
import com.gmall.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/order")
public class OrderApiController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private RedisTemplate redisTemplate;



    // 去结算
    @GetMapping("/auth/trade")
    public Result<Map> toTradePage(HttpServletRequest request) {
        String userId = AuthContextHolder.getUserId(request);
        Map map = orderService.toTradePage(userId);
        return Result.ok(map);
    }

    @PostMapping("/auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo, String tradeNo, HttpServletRequest request) {
        String userId = AuthContextHolder.getUserId(request);
        orderInfo.setUserId(Long.parseLong(userId));
        // 校验交易号是否为第一次提交
        String tradeNoCacheKey = "trade:" + userId;
        String cacheTradeNo = (String) redisTemplate.opsForValue().get(tradeNoCacheKey);
        if (StringUtils.isEmpty(tradeNoCacheKey) || !cacheTradeNo.equals(tradeNo)) {
            // 重复提交
            return Result.fail("请勿重复提交");
        }
        // 删除redis中的交易号缓存
        redisTemplate.delete(tradeNoCacheKey);
        // 校验库存是否足够
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            boolean isFlag = orderService.checkStock(orderDetail);
                if (!isFlag) {
                    return Result.fail().message(orderDetail.getSkuName() + "[库存不足]");
                }
        }
        //  生成订单号保存订单表并保存订单详情表
        Long orderId =  orderService.saveOrderInfo(orderInfo);

        // 返回订单号
        return Result.ok(orderId);
    }

    @GetMapping("/auth/getOrderInfo/{orderId}")
    public OrderInfo getOrderInfo(@PathVariable String orderId) {
        return orderService.getOrcderInfo(orderId);
    }

    //开始拆单
    @PostMapping("/orderSplit")
    public String orderSplit(Long orderId, String wareSkuMap) {
        List<OrderInfo> subOrderInfoList = orderService.orderSplit(orderId, wareSkuMap);
        // 将其转化为Json相应库存系统
        List<Map> listMap = subOrderInfoList.stream().map(subOrderInfo -> {
            return orderService.initWareData(subOrderInfo);
        }).collect(Collectors.toList());
        return JSONObject.toJSONString(listMap);
    }
}
