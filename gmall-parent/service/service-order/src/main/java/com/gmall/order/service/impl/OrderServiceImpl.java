package com.gmall.order.service.impl;

import com.gmall.cart.client.CartFeignClient;
import com.gmall.common.util.HttpClientUtil;
import com.gmall.model.cart.CartInfo;
import com.gmall.model.enums.OrderStatus;
import com.gmall.model.enums.ProcessStatus;
import com.gmall.model.order.OrderDetail;
import com.gmall.model.order.OrderInfo;
import com.gmall.model.user.UserAddress;
import com.gmall.order.mapper.OrderDetailMapper;
import com.gmall.order.mapper.OrderInfoMapper;
import com.gmall.order.service.OrderService;
import com.gmall.product.client.ProductFeignClient;
import com.gmall.user.client.UserFeignClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private CartFeignClient cartFeignClient;

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Value("${ware.url}")
    private String url;


    // 去结算，查询结算页面所需数据
    @Override
    public Map toTradePage(String userId) {
        Map map = new HashMap();
        // 1.查询当前用户所有收货地址
        List<UserAddress> userAddressList = userFeignClient.getUserAddressList(userId);
        map.put("userAddressList", userAddressList);
        // 2.查询购物车中所选中的商品
        // 构建redis中的缓存key
        List<CartInfo> cartInfos = cartFeignClient.getCartCheckedList(userId);
        // 获取选中的购物项
        List<CartInfo> cartInfoList = cartInfos.stream().filter(cartInfo -> {
            if (1 == cartInfo.getIsChecked()) {
                return true;
            }
            return false;
        }).peek(cartInfo -> {
            cartInfo.setSkuPrice(productFeignClient.getSkuPrice(cartInfo.getSkuId()));
        }).collect(Collectors.toList());
        // 将购物项集合转换为订单详情
        List<OrderDetail> detailArrayList = cartInfoList.stream().map(cartInfo -> {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cartInfo, orderDetail);
            orderDetail.setOrderPrice(cartInfo.getSkuPrice());
            return orderDetail;
        }).collect(Collectors.toList());
        map.put("detailArrayList", detailArrayList);
        // 3. 计算出总金额、总件数
        BigDecimal totalAmount = new BigDecimal(0);
        Long totalNum = detailArrayList.stream().collect(
                Collectors.summarizingInt(OrderDetail::getSkuNum)).getSum();
        for (OrderDetail orderDetail : detailArrayList) {
            totalAmount = totalAmount.add(orderDetail.getOrderPrice()
                    .multiply(new BigDecimal(orderDetail.getSkuNum())));
           //方式一:totalNum += orderDetail.getSkuNum();
        }
        map.put("totalAmount", totalAmount);
        map.put("totalNum", totalNum);
        // 4.生成交易号或订单号并保存到redis中,并传递到页面微服务
        String tradeNo = UUID.randomUUID().toString().replaceAll("-", "");
        // 交易号缓存key
        String tradeNoCacheKey = "trade:" + userId;
        redisTemplate.opsForValue().set(tradeNoCacheKey, tradeNo);
        map.put("tradeNo", tradeNo);
        return map;
    }

    @Override
    public boolean checkStock(OrderDetail orderDetail) {

           return  "1".equals(HttpClientUtil.doGet(url + "/hasStock?skuId=" + orderDetail.getSkuId()
                    + "&num=" + orderDetail.getSkuNum()));

    }

    // 保存订单表
    @Override
    @Transactional
    public Long saveOrderInfo(OrderInfo orderInfo) {
        // 设置订单状态
        orderInfo.setOrderStatus(OrderStatus.UNPAID.name());
        // 设置订单交易编码
        String outTradeNo = "gmall" + System.currentTimeMillis()
                + new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        // 设置订单描述
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();

        StringBuilder tradeBody = new StringBuilder();
        orderDetailList.stream().forEach(orderDetail ->  {
            tradeBody.append(orderDetail.getSkuName());
            orderDetail.setOrderPrice(productFeignClient.getSkuPrice(orderDetail.getSkuId()));
        });
        // 设置总金额
        orderInfo.sumTotalAmount();
        orderInfo.setTotalAmount(orderInfo.getTotalAmount());
        // 设置订单描述
        if (tradeBody.length() > 100) {
            orderInfo.setTradeBody(tradeBody.substring(0, 100));
        } else {
            orderInfo.setTradeBody(tradeBody.toString());
        }
        // 设置创建时间及过期时间
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
        orderInfo.setCreateTime(calendar.getTime());
        calendar.add(Calendar.HOUR, 2);
        orderInfo.setExpireTime(calendar.getTime());
        // 设置进度状态
        orderInfo.setProcessStatus(ProcessStatus.UNPAID.name());
        // 保存到order_info
        orderInfoMapper.insert(orderInfo);
        // 保存到订单详情表
        orderDetailList.stream().forEach(orderDetail ->  {
            // 设置外键为订单Id
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insert(orderDetail);
        });
        return orderInfo.getId();
    }
}
