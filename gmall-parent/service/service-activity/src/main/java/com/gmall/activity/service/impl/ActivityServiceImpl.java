package com.gmall.activity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gmall.activity.mapper.SeckillGoodsMapper;
import com.gmall.activity.service.ActivityService;
import com.gmall.activity.utils.DateUtil;
import com.gmall.common.constant.RedisConst;
import com.gmall.model.activity.SeckillGoods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;

@Service
public class ActivityServiceImpl implements ActivityService {
    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    // 上架当天的秒杀商品
    @Override
    public void secKillOnSale() {
        // 先判断当天的数据是否已经被放入redis缓存中
        // 取出当天的数据
        List<SeckillGoods> goodsList = seckillGoodsMapper.selectList(new QueryWrapper<SeckillGoods>()
                .eq("status", 1)
                .gt("stock_count", 0)
                .eq("DATE_FORMAT(start_time,'%Y-%m-%d')", DateUtil.formatDate(new Date()))
        );
        if (!CollectionUtils.isEmpty(goodsList)) {
            goodsList.stream().forEach(goods -> {
                // 防止重复上架同一天的秒杀商品
                if (!redisTemplate.opsForHash().hasKey(RedisConst.SECKILL_GOODS, goods.getSkuId().toString())) {
                    // 若缓存中没有则放入缓存
                    redisTemplate.opsForHash().put(RedisConst.SECKILL_GOODS, goods.getSkuId().toString(), goods);
                    // 往缓存中放入num个当前商品，方式超卖
                    String[] ids = buildIds(goods);
                    redisTemplate.opsForList().leftPushAll(RedisConst.SECKILL_STOCK_PREFIX
                            + goods.getSkuId(), ids);
                    //TODO 3:状态位 设置状态位的状态位可秒杀 skuId:1
                    redisTemplate.convertAndSend("seckillpush", goods.getSkuId() + ":1");
                }
            });
        }

    }

    // 返回所有正在秒杀的商品集合
    @Override
    public List<SeckillGoods> getSecKillGoodsList() {
       return redisTemplate.opsForHash().values(RedisConst.SECKILL_GOODS);
    }

    // 获取当前商品的详情
    @Override
    public SeckillGoods getSecKillGoodsInfo(String skuId) {
       return (SeckillGoods) redisTemplate.opsForHash().get(RedisConst.SECKILL_GOODS, skuId);
    }

    private String[] buildIds(SeckillGoods goods) {
        Integer num = goods.getNum();
        String[] ids = new String[num];
        for (int i = 0; i < num; i++) {
            ids[i] = goods.getSkuId().toString();
        }
        return ids;
    }
}
