package com.gmall.item.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.gmall.item.service.ItemService;
import com.gmall.model.product.BaseCategoryView;
import com.gmall.model.product.SkuInfo;
import com.gmall.model.product.SpuSaleAttr;
import com.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ItemServiceImpl implements ItemService {

    @Autowired
    private ProductFeignClient productFeignClient;
    // 获取sku详情信息
    @Override
    public Map getgetSkuInfo(Long skuId) {
        HashMap<Object, Object> result = new HashMap<>();

        // 获取SkuInfo
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        if (null == skuInfo) {
            return null;
        }
        result.put("skuInfo", skuInfo);
        // 获取分类信息
        BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
        result.put("categoryView", categoryView);

        // 获取销售价格
        BigDecimal price = productFeignClient.getSkuPrice(skuId);
        result.put("price", price);

        // 获取销售属性及销售属性值
        List<SpuSaleAttr> spuSaleAttrList = productFeignClient.
                selectSpuSaleAttrListCheckBySku(skuInfo.getSpuId(), skuInfo.getId());
        result.put("spuSaleAttrList",spuSaleAttrList);
        //5:组合数据 对应 SKUID   页面上要的是JSON串
        Map saleAttrValuesBySpu = productFeignClient.getSaleAttrValuesBySpu(skuInfo.getSpuId());
        result.put("valuesSkuJson", JSONObject.toJSONString(saleAttrValuesBySpu));

        //返回汇总结果
        return result;
    }
}
