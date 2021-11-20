package com.gmall.web.controller;

import com.gmall.item.client.ItemFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
@RequestMapping
public class ItemController {

    @Autowired
    private ItemFeignClient itemFeignClient;
    /**
     * sku详情页面
     * @param skuId
     * @param model
     * @return
     */

    @GetMapping("/{skuId}.html")
    public String getItem(@PathVariable Long skuId, Model model) {
        Map skuInfo = itemFeignClient.getSkuInfo(skuId);
        model.addAllAttributes(skuInfo);
        return "item/index";
    }
}
