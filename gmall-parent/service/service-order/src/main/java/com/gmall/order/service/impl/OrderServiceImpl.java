package com.gmall.order.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import com.gmall.order.service.OrderAsyncService;
import com.gmall.order.service.OrderService;
import com.gmall.product.client.ProductFeignClient;
import com.gmall.rabbitmq.constant.MqConst;
import com.gmall.rabbitmq.service.RabbitMQService;
import com.gmall.user.client.UserFeignClient;
import com.google.common.collect.Ordering;
import org.apache.catalina.webresources.WarResource;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
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

    @Autowired
    private OrderAsyncService orderAsyncService;

    @Autowired
    private RabbitMQService rabbitMQService;

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
        // 异步 删除数据库及redis中的购物项
        orderAsyncService.deleteCartList(orderDetailList);
        // 发送定时消息，
        rabbitMQService.sentDelayedMessage(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL,
                MqConst.ROUTING_ORDER_CANCEL, orderInfo.getId(), 30*1000*60);
        System.out.println("延时消息发送成功");

        return orderInfo.getId();
    }

    // 获取订单信息
    @Override
    public OrderInfo getOrcderInfo(String orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        List<OrderDetail> orderDetailList = orderDetailMapper.selectList(new QueryWrapper<OrderDetail>()
                .eq("order_id", orderId));
        orderInfo.setOrderDetailList(orderDetailList);
        return orderInfo;
    }

    @Override
    public void updateOrderInfo(String orderId, ProcessStatus processStatus) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setProcessStatus(processStatus.name());
        orderInfo.setOrderStatus(processStatus.getOrderStatus().name());
        orderInfo.setId(Long.parseLong(orderId));
        orderInfoMapper.updateById(orderInfo);
    }

    @Override
    public Map initWareData(String orderId) {
        OrderInfo orcderInfo = getOrcderInfo(orderId);
        Map map = initWareData(orcderInfo);
        return map;
    }

    @Override
    public Map initWareData(OrderInfo orderInfo) {
        Map map = new HashMap<String, Object>();
        map.put("orderId", orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel", orderInfo.getConsigneeTel());
        map.put("orderComment", orderInfo.getOrderComment());
        map.put("orderBody", orderInfo.getTradeBody());
        map.put("deliveryAddress", orderInfo.getDeliveryAddress());
        map.put("paymentWay", "ONLINE".equals(orderInfo.getPaymentWay()) ? 2 : 1);
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        List<HashMap> orderDetailMap = orderDetailList.stream().map(orderDetail -> {
            HashMap orderDetailListMap = new HashMap();
            orderDetailListMap.put("skuId", orderDetail.getSkuId());
            orderDetailListMap.put("skuNum", orderDetail.getSkuNum());
            orderDetailListMap.put("skuName", orderDetail.getSkuName());
            return orderDetailListMap;
        }).collect(Collectors.toList());
        map.put("details", orderDetailMap);
        return map;
    }

    // 拆单并返回Json串
    @Override
    public List<OrderInfo> orderSplit(Long orderId, String wareSkuMap) {
        // 获取当前订单的详细信息
        OrderInfo originOrderInfo = getOrcderInfo(String.valueOf(orderId));
        //
        List<Map> wareSkuMapList = JSONObject.parseArray(wareSkuMap, Map.class);
        List  subOrderInfoList = new ArrayList<OrderInfo>();
        if (!CollectionUtils.isEmpty(wareSkuMapList)) {
            for (Map map : wareSkuMapList) {
                OrderInfo subOrderInfo = new OrderInfo();
                BeanUtils.copyProperties(originOrderInfo, subOrderInfo);
                subOrderInfo.setId(null);
                subOrderInfo.setParentOrderId(originOrderInfo.getId());
                subOrderInfo.setWareId((String) map.get("wareId"));
                // 获取所有订单详情
                List<OrderDetail> orderDetailList = originOrderInfo.getOrderDetailList();
                // 获取每个Map中的订单详情
                List<OrderDetail> subOrderDetailList = orderDetailList.stream().filter(orderDetail -> {
                    List<String> skuIds = (List<String>) map.get("skuIds");
                    if (skuIds.contains(orderDetail.getSkuId().toString())) {
                        return true;
                    }
                    return false;
                }).collect(Collectors.toList());
                // 设置子订单的订单详情
                subOrderInfo.setOrderDetailList(subOrderDetailList);
                // 设置子订单的总金额
                subOrderInfo.sumTotalAmount();
                subOrderInfo.setTotalAmount(subOrderInfo.getTotalAmount());
                // 保存子订单
                orderInfoMapper.insert(subOrderInfo);
                //更新订单详情表中的OrderId为新生成的OrderId
                subOrderDetailList.stream().forEach(subOrderDetail -> {
                    subOrderDetail.setOrderId(subOrderInfo.getId());
                    orderDetailMapper.updateById(subOrderDetail);
                });
                // 将其添加到list中相应数据
                subOrderInfoList.add(subOrderInfo);
            }
            updateOrderInfo(String.valueOf(orderId), ProcessStatus.SPLIT);
        }
        return subOrderInfoList;
    }


}
