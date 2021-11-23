package com.gmall.item.controller;

import com.gmall.item.service.ItemService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

@RestController
@Api("商品详情")
@RequestMapping("/api/item")
public class ItemApiController {

    @Autowired
    private ItemService itemService;


    /**
     * 获取sku详情信息
     *
     * @return
     */
    @ApiOperation("获取sku详情信息")
    @GetMapping("/getItem/{skuId}")
    public Map getSkuInfo(@PathVariable Long skuId) {
        Map map = itemService.getgetSkuInfo(skuId);
        return map;
    }

}
