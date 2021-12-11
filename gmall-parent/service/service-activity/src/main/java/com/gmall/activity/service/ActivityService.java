package com.gmall.activity.service;

import com.gmall.model.activity.SeckillGoods;

import java.util.List;

public interface ActivityService {
    void secKillOnSale();


    List<SeckillGoods> getSecKillGoodsList();

    SeckillGoods getSecKillGoodsInfo(String skuId);
}
