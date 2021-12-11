package com.gmall.activity;

import com.gmall.model.activity.SeckillGoods;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient("service-activityyc")
public interface ActivityFeignClient {
    @GetMapping("/api/activity/seckill/getSecKillGoodsList")
    public List<SeckillGoods> getSecKillGoodsList();

    @GetMapping("/api/activity/seckill/getSecKillGoodsInfo/{skuId}")
    public SeckillGoods getSecKillGoodsInfo(@PathVariable String skuId);
}
