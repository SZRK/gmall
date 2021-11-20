package com.gmall.item.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "service-itemyc")
public interface ItemFeignClient {
    /**
     * 获取sku详情信息
     * @return
     */
    @GetMapping("/api/item/getItem/{skuId}")
    public Map getSkuInfo(@PathVariable("skuId") Long skuId);
}
