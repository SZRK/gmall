package com.gmall.activity.controller;

import com.gmall.activity.redis.CacheHelper;
import com.gmall.activity.service.ActivityService;
import com.gmall.common.constant.RedisConst;
import com.gmall.common.result.Result;
import com.gmall.common.result.ResultCodeEnum;
import com.gmall.common.util.AuthContextHolder;
import com.gmall.common.util.MD5;
import com.gmall.model.activity.SeckillGoods;
import com.gmall.model.activity.UserRecode;
import com.gmall.model.list.Goods;
import com.gmall.rabbitmq.constant.MqConst;
import com.gmall.rabbitmq.service.RabbitMQService;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.AccessibleObject;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/activity/seckill")
public class ActivityApiController {

    @Autowired
    private ActivityService activityService;

    @Autowired
    private RabbitMQService rabbitMQService;

    @Autowired
    private RedisTemplate redisTemplate;

    @GetMapping("/getSecKillGoodsList")
    public List<SeckillGoods> activityService() {
        return activityService.getSecKillGoodsList();
    }

    @GetMapping("/getSecKillGoodsInfo/{skuId}")
    public SeckillGoods getSecKillGoodsInfo(@PathVariable String skuId) {
        return activityService.getSecKillGoodsInfo(skuId);
    }

    // 获取抢购码
    @GetMapping("/auth/getSeckillSkuIdStr/{skuId}")
    public Result getSeckillSkuIdStr(@PathVariable String skuId, HttpServletRequest request) {

        String userId = AuthContextHolder.getUserId(request);
        SeckillGoods secKillGoodsInfo = activityService.getSecKillGoodsInfo(skuId);
        // 判断当前是否通过审核
        if (SeckillGoods.NOTSTATUS.equals(secKillGoodsInfo.getStatus())) {
            // 当前商品没有通过审核
            return Result.fail().message("商品已下架");
        }
        //该商品当前是否可以抢购
        Date startTime = secKillGoodsInfo.getStartTime();
        Date endTime = secKillGoodsInfo.getEndTime();
        Date nowTime = new Date();

        if (nowTime.before(startTime) || nowTime.after(endTime)) {
            // 当前秒杀商品在当前时间已经不能秒杀
            return Result.fail().message("该商品当前不能秒杀");
        }
        // 判断当前商品库存是否大于0
        if (secKillGoodsInfo.getStockCount() <= 0) {
            return Result.build(null, ResultCodeEnum.SECKILL_FINISH);
        }
        // 当前商品可以秒杀, 发放抢购码  MD5(userId+“：” +skuId) 算法
        String skuIdStr  = MD5.encrypt(userId + ":" + skuId);
        // 存入redis中
        return Result.ok(skuIdStr);
    }

    // 秒杀一次校验
    @PostMapping("/auth/seckillOrder")
    public Result seckillOrder(String skuId, String skuIdstr, HttpServletRequest request) {
        String userId = AuthContextHolder.getUserId(request);
        String encrypt = MD5.encrypt(userId + ":" + skuId);
        // 判断抢购码
        if (StringUtils.isEmpty(skuIdstr) || !skuIdstr.equals(encrypt)) {
            // 抢购码为空或者抢购码不匹配
            return Result.build(null, ResultCodeEnum.SECKILL_ILLEGAL);
        }
        // 判断状态位：本地缓存中去出状态位
        String status = (String) CacheHelper.get(skuId);
        if ("0".equals(status)) {
            // 已经没有货了
            return Result.build(null, ResultCodeEnum.SECKILL_FINISH);
        }
        // 发送消息：削峰
        UserRecode userRecode = new UserRecode();
        userRecode.setSkuId(Long.parseLong(skuId));
        userRecode.setUserId(userId);
        rabbitMQService.sentMessage(MqConst.EXCHANGE_DIRECT_SECKILL_USER, MqConst.ROUTING_SECKILL_USER, userRecode);
        return Result.ok();
    }

    // 每3秒检查一次当前状态
    @GetMapping("/auth/checkOrder/{skuId}")
    public Result checkOrder(@PathVariable String skuId, HttpServletRequest request) {
        String userId = AuthContextHolder.getUserId(request);
        // 判断用户是否来过
        Object result1 = redisTemplate.opsForValue().get(RedisConst.SECKILL_USER
                + userId + ":" + skuId);
        if (null == result1) {
            // 用户没来过 判段当前的状态位
            String result2 = (String) CacheHelper.get(skuId);
            if ("0".equals(result2)) {
                // 已售罄
                return Result.build(null, ResultCodeEnum.SECKILL_FINISH);
            }
            // 状态位为1，且用户没来过，说明在排队中
            return Result.build(null, ResultCodeEnum.SECKILL_RUN);
        }
        // 用户来过, 校验用户是否已经下过单
        Object result3 =redisTemplate.opsForHash().get(RedisConst.SECKILL_ORDERS_USERS + userId, skuId);
        if (null != result3) {
            // 用户已经下过单
            return Result.build(null, ResultCodeEnum.SECKILL_ORDER_SUCCESS);
        }
        // 用户没下单，检查用户是否具有购买资格
        Object result4 = redisTemplate.opsForHash().get(RedisConst.SECKILL_ORDERS + userId, skuId);
        if (null != result4) {
            // 用户具有购买资格
            return Result.build(null, ResultCodeEnum.SECKILL_SUCCESS);
        }
        return Result.build(null, ResultCodeEnum.SECKILL_FAIL);
    }

}
